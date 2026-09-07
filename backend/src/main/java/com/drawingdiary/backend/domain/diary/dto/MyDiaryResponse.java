package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.diary.Visibility;

import java.time.LocalDateTime;

public record MyDiaryResponse(
        Long id,
        String title,
        String content,
        String thumbnailUrl,
        LocalDateTime createdAt,
        Long categoryId,
        String categoryName,
        Visibility visibility
) {
}
