package com.drawingdiary.backend.domain.auth.dto;

public record SignupResponse(
        Long userId,
        String loginId,
        String nickname
) {
}
