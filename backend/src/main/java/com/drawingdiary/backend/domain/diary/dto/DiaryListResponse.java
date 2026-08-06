package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.diary.Visibility;

import java.time.LocalDateTime;

public record DiaryListResponse(
        Long id,
        String title,
        Long authorId,
        String authorNickname,
        LocalDateTime createdAt,
        String thumbnailUrl,
        Visibility visibility
) {
}
