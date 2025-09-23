package com.chill_guys.vitae_backend.user.model.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken, // opaque "<SessionId>.<secret>"
        String userId,
        UserDTO user
) {
}
