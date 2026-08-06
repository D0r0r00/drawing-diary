package com.drawingdiary.backend.domain.room.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RoomInviteRequest(
        @NotEmpty(message = "초대할 사용자를 선택해주세요.")
        List<Long> invitedUserIds
) {
}
