package com.drawingdiary.backend.domain.aiscore.dto;

public record AiScoreResponse(
        Long diaryId,
        int relevanceScore,
        int colorScore,
        int likeScore,
        int totalScore
) {
}
