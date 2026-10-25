package vn.edu.demo.userservice.api;

import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.userservice.domain.AvatarUploadRequest;
import vn.edu.demo.userservice.domain.AvatarUploadResponse;
import vn.edu.demo.userservice.domain.User;
import vn.edu.demo.userservice.domain.UserStore;
import vn.edu.demo.userservice.storage.MinioStorageService;

@RestController
@RequestMapping("/users/{userId}/avatar")
public class AvatarController {
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final UserStore users;
    private final MinioStorageService minioStorage;

    public AvatarController(UserStore users, MinioStorageService minioStorage) {
        this.users = users;
        this.minioStorage = minioStorage;
    }

    @PostMapping("/upload-url")
    public AvatarUploadResponse createUploadUrl(
            @PathVariable String userId,
            @Valid @RequestBody AvatarUploadRequest request) {
        User user = users.findById(userId);
        String contentType = request.contentType().trim().toLowerCase(Locale.ROOT);
        String extension = IMAGE_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP");
        }

        String objectKey = "avatars/" + user.id() + "/" + UUID.randomUUID() + "." + extension;
        String uploadUrl = minioStorage.createAvatarUploadUrl(objectKey, contentType);
        users.updateAvatarKey(userId, objectKey);
        return new AvatarUploadResponse(
                "PUT",
                uploadUrl,
                objectKey,
                MinioStorageService.URL_EXPIRY_SECONDS,
                Map.of(HttpHeaders.CONTENT_TYPE, contentType));
    }
}
