package com.drawingdiary.backend.domain.user.dto;

public record UserSearchResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {
}
