package com.drawingdiary.backend.domain.aiscore.dto;

public record MyRankingItemResponse(
        Long diaryId,
        int rank,
        int totalScore
) {
}
