package vn.edu.demo.userservice.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketCorsArgs;
import io.minio.http.Method;
import io.minio.messages.CORSConfiguration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MinioStorageService {
    public static final int URL_EXPIRY_SECONDS = 300;

    private final MinioClient internalClient;
    private final MinioClient presignClient;
    private final String bucket;
    private final List<String> corsOrigins;

    public MinioStorageService(
            @Value("${minio.internal-endpoint}") String internalEndpoint,
            @Value("${minio.public-endpoint}") String publicEndpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket}") String bucket,
            @Value("${minio.cors-origins}") String corsOrigins) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            throw new IllegalStateException("MINIO_ACCESS_KEY và MINIO_SECRET_KEY phải được cấu hình");
        }
        this.internalClient = createClient(internalEndpoint, accessKey, secretKey);
        this.presignClient = createClient(publicEndpoint, accessKey, secretKey);
        this.bucket = bucket;
        this.corsOrigins = List.of(corsOrigins.split(",")).stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        if (this.corsOrigins.isEmpty()) {
            throw new IllegalStateException("Cần cấu hình ít nhất một MINIO_CORS_ORIGINS");
        }
    }

    public void initializeBucket() {
        try {
            if (!internalClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                internalClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            configureBucketCors();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể khởi tạo bucket MinIO", exception);
        }
    }

    public String createAvatarUploadUrl(String objectKey, String contentType) {
        try {
            return presignClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(URL_EXPIRY_SECONDS)
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .build());
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Không thể tạo URL tải ảnh lên MinIO",
                    exception);
        }
    }

    private void configureBucketCors() throws Exception {
        CORSConfiguration.CORSRule rule = new CORSConfiguration.CORSRule(
                List.of("*"),
                List.of("PUT"),
                corsOrigins,
                List.of("ETag"),
                "avatar-upload",
                300);
        CORSConfiguration configuration = new CORSConfiguration(List.of(rule));
        internalClient.setBucketCors(
                SetBucketCorsArgs.builder().bucket(bucket).config(configuration).build());
    }

    private static MinioClient createClient(String endpoint, String accessKey, String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
