package com.drawingdiary.backend.domain.room.dto;

public record RoomMemberResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
