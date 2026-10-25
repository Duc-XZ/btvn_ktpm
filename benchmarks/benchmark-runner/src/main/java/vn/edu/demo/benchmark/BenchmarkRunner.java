package vn.edu.demo.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class BenchmarkRunner implements CommandLineRunner {
    private final Environment environment;
    private final ObjectMapper json = new ObjectMapper();

    public BenchmarkRunner(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        String mode = environment.getProperty("mode", "benchmark");
        if (mode.equalsIgnoreCase("token")) {
            System.out.println(createToken());
            return;
        }
        if (!mode.equalsIgnoreCase("benchmark")) {
            throw new IllegalArgumentException("mode chỉ nhận token hoặc benchmark");
        }
        System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(runBenchmark()));
    }

    private String createToken() throws Exception {
        String secret = required("JWT_SECRET");
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET cần có ít nhất 32 byte UTF-8");
        }
        String subject = environment.getProperty("subject", "u-123");
        long now = System.currentTimeMillis() / 1000;
        long ttl = Long.parseLong(environment.getProperty("ttl-seconds", "900"));
        if (ttl < 1) {
            throw new IllegalArgumentException("ttl-seconds phải lớn hơn 0");
        }
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", subject);
        claims.put("iat", now);
        claims.put("exp", now + ttl);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String unsigned = encodeJson(encoder, header) + "." + encodeJson(encoder, claims);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return unsigned + "." + encoder.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.US_ASCII)));
    }

    private String encodeJson(Base64.Encoder encoder, Object value) throws Exception {
        return encoder.encodeToString(json.writeValueAsBytes(value));
    }

    private Map<String, Object> runBenchmark() throws Exception {
        int requests = intProperty("requests", 100);
        int warmup = intProperty("warmup", 5);
        double rate = Double.parseDouble(environment.getProperty("rate", "5"));
        if (requests < 1 || warmup < 0 || rate <= 0 || rate > 9) {
            throw new IllegalArgumentException("Cần requests >= 1, warmup >= 0 và rate trong khoảng (0, 9]");
        }
        int timeoutMillis = intProperty("timeout-ms", 5000);
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeout-ms phải lớn hơn 0");
        }
        Duration timeout = Duration.ofMillis(timeoutMillis);
        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
        String directUrl = environment.getProperty("direct-url", "http://localhost:8081/users/u-123");
        String gatewayUrl = environment.getProperty("gateway-url", "http://localhost:8080/users/u-123");
        String token = required("JWT_TOKEN");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("createdAt", java.time.Instant.now().toString());
        result.put("requests", requests);
        result.put("warmup", warmup);
        result.put("ratePerSecond", rate);
        result.put("direct", measure(client, directUrl, null, requests, warmup, rate, timeout));
        result.put("gateway", measure(client, gatewayUrl, token, requests, warmup, rate, timeout));
        result.put("gatewayMinusDirectP50Ms", difference(result, "p50Ms"));
        result.put("gatewayMinusDirectP95Ms", difference(result, "p95Ms"));
        String output = environment.getProperty("output");
        if (output != null && !output.isBlank()) {
            Path path = Path.of(output);
            if (path.toAbsolutePath().getParent() != null) Files.createDirectories(path.toAbsolutePath().getParent());
            json.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), result);
        }
        return result;
    }

    private Object measure(HttpClient client, String url, String token, int count, int warmup,
                           double rate, Duration timeout) throws InterruptedException {
        List<Double> samples = new ArrayList<>();
        Map<Integer, Integer> statuses = new TreeMap<>();
        int errors = 0;
        long interval = (long) (1_000_000_000d / rate);
        for (int i = 0; i < warmup; i++) {
            try { send(client, url, token, timeout); } catch (Exception ignored) { }
        }
        long started = System.nanoTime();
        long next = started;
        for (int i = 0; i < count; i++) {
            long delay = next - System.nanoTime();
            if (delay > 0) sleepNanos(delay);
            long requestStarted = System.nanoTime();
            try {
                int status = send(client, url, token, timeout);
                samples.add((System.nanoTime() - requestStarted) / 1_000_000d);
                statuses.merge(status, 1, Integer::sum);
            } catch (Exception exception) {
                errors++;
            }
            next += interval;
        }
        double elapsedSeconds = (System.nanoTime() - started) / 1_000_000_000d;
        Map<String, Object> metrics = new LinkedHashMap<>();
        long successful = statuses.entrySet().stream().filter(e -> e.getKey() >= 200 && e.getKey() < 300)
                .mapToLong(Map.Entry::getValue).sum();
        metrics.put("attempted", count);
        metrics.put("successful2xx", successful);
        metrics.put("httpErrors", statuses.values().stream().mapToInt(Integer::intValue).sum() - successful);
        metrics.put("transportErrors", errors);
        metrics.put("statusCodes", statuses);
        metrics.put("elapsedSeconds", round(elapsedSeconds));
        metrics.put("successfulRequestsPerSecond", round(successful / elapsedSeconds));
        metrics.put("p50Ms", percentile(samples, .50));
        metrics.put("p95Ms", percentile(samples, .95));
        metrics.put("p99Ms", percentile(samples, .99));
        return metrics;
    }

    private int send(HttpClient client, String url, String token, Duration timeout) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static void sleepNanos(long nanos) throws InterruptedException {
        long millis = nanos / 1_000_000;
        int remainder = (int) (nanos % 1_000_000);
        Thread.sleep(millis, remainder);
    }

    private static Double percentile(List<Double> values, double quantile) {
        if (values.isEmpty()) return null;
        List<Double> sorted = values.stream().sorted().toList();
        int index = Math.max(0, (int) Math.ceil(quantile * sorted.size()) - 1);
        return round(sorted.get(index));
    }

    private static double round(double value) { return Math.round(value * 1000d) / 1000d; }

    @SuppressWarnings("unchecked")
    private static Double difference(Map<String, Object> result, String key) {
        Map<String, Object> direct = (Map<String, Object>) result.get("direct");
        Map<String, Object> gateway = (Map<String, Object>) result.get("gateway");
        Double a = (Double) direct.get(key);
        Double b = (Double) gateway.get(key);
        return a == null || b == null ? null : round(b - a);
    }

    private int intProperty(String name, int fallback) {
        return Integer.parseInt(environment.getProperty(name, Integer.toString(fallback)));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Thiếu biến môi trường " + name);
        return value;
    }
}
