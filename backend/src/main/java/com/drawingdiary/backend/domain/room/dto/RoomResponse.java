package com.drawingdiary.backend.domain.room.dto;

import com.drawingdiary.backend.domain.room.RoomStatus;

import java.util.List;

public record RoomResponse(
        Long roomId,
        RoomStatus status,
        Long ownerId,
        List<RoomMemberResponse> members
) {
}
