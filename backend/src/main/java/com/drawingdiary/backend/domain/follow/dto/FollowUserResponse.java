package com.drawingdiary.backend.domain.follow.dto;

public record FollowUserResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
