package com.drawingdiary.backend.domain.room.dto;

import java.time.LocalDateTime;

public record RoomCanvasResponse(
        Long roomId,
        String canvasData,
        String title,
        String content,
        LocalDateTime updatedAt
) {
}
