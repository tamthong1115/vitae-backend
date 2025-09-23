package com.chill_guys.vitae_backend.user.model.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutAllRequest(
        @NotBlank String userId
) {
}
