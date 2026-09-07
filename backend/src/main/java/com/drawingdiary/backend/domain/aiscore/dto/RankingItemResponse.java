package com.drawingdiary.backend.domain.aiscore.dto;

public record RankingItemResponse(
        int rank,
        Long diaryId,
        String title,
        String thumbnailUrl,
        int totalScore,
        Long authorId,
        String authorNickname
) {
}
