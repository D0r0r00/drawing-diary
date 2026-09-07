package com.drawingdiary.backend.domain.room.dto;

import java.time.LocalDateTime;

public record RoomCanvasSaveResponse(
        Long roomId,
        LocalDateTime savedAt
) {
}
