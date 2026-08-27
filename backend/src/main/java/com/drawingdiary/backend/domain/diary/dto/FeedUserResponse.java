package com.drawingdiary.backend.domain.diary.dto;

public record FeedUserResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
