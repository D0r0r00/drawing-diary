package com.drawingdiary.backend.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId
) {
}
