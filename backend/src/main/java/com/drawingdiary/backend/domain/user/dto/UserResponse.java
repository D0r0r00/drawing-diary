package com.drawingdiary.backend.domain.user.dto;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl
) {
}
