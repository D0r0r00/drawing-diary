package com.drawingdiary.backend.domain.auth.dto;

public record TokenRefreshResponse(
        String accessToken,
        Long userId
) {
}
