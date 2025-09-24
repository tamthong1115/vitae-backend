package com.chill_guys.vitae_backend.user.model.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) {
}
