package com.chill_guys.vitae_backend.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Email @NotBlank String email,
        String accountName,
        @NotBlank String password
) {
}
