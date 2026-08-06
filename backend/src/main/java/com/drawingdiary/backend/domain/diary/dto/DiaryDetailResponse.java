package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.diary.Visibility;

import java.time.LocalDateTime;

public record DiaryDetailResponse(
        Long id,
        String title,
        String textContent,
        String canvasData,
        String imageUrl,
        Visibility visibility,
        LocalDateTime createdAt
) {
}
