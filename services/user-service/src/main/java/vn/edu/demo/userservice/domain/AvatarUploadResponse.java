package vn.edu.demo.userservice.domain;

import java.util.Map;

public record AvatarUploadResponse(
        String method,
        String uploadUrl,
        String objectKey,
        int expiresInSeconds,
        Map<String, String> requiredHeaders) {
}
