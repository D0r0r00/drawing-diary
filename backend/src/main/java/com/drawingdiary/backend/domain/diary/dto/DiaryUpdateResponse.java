package com.drawingdiary.backend.domain.diary.dto;

import java.time.LocalDateTime;

public record DiaryUpdateResponse(
        Long id,
        String title,
        LocalDateTime updatedAt
) {
}
