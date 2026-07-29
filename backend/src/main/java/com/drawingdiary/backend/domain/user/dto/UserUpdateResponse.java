package com.drawingdiary.backend.domain.user.dto;

public record UserUpdateResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {
}
