package vn.edu.demo.userservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public JwtService(ObjectMapper objectMapper, @Value("${app.jwt.secret:}") String secret) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
    }

    public boolean isValidBearer(String authorization) {
        if (authorization == null || authorization.length() < 7
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return false;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.length() <= 8192 && isValidToken(token);
    }

    private boolean isValidToken(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            return false;
        }

        try {
            JsonNode header = readJson(parts[0]);
            if (!"HS256".equals(header.path("alg").asText())) {
                return false;
            }

            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expectedSignature, suppliedSignature)) {
                return false;
            }

            JsonNode claims = readJson(parts[1]);
            if (!claims.path("sub").isTextual() || claims.path("sub").asText().isBlank()) {
                return false;
            }

            long now = System.currentTimeMillis() / 1000;
            JsonNode expiration = claims.get("exp");
            if (expiration == null || !expiration.isIntegralNumber() || expiration.asLong() <= now) {
                return false;
            }

            JsonNode notBefore = claims.get("nbf");
            return notBefore == null || (notBefore.isIntegralNumber() && notBefore.asLong() <= now);
        } catch (Exception exception) {
            return false;
        }
    }

    private JsonNode readJson(String encodedPart) throws Exception {
        byte[] decoded = Base64.getUrlDecoder().decode(encodedPart);
        return objectMapper.readTree(decoded);
    }

    private byte[] sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
    }
}
