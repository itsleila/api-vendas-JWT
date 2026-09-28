package com.example.auth_service.dto;

public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
