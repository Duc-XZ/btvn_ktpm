package vn.edu.demo.userservice.domain;

import jakarta.validation.constraints.NotBlank;

public record AvatarUploadRequest(@NotBlank String contentType) {
}
