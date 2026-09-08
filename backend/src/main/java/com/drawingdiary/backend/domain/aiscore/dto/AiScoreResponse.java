package com.drawingdiary.backend.domain.aiscore.dto;

/**
 * 수동 저장·조회·AI 자동 산정 세 경로가 모두 이 형태로 응답한다.
 *
 * @param feedback AI 서버가 점수와 함께 준 평가 코멘트(ai_scores.ai_comment). 점수 계산에는
 *                 쓰이지 않는 기록이다. 아직 AI 산정을 돌리지 않았거나 코멘트가 비어 있으면 null.
 */
public record AiScoreResponse(
        Long diaryId,
        int relevanceScore,
        int colorScore,
        int likeScore,
        int totalScore,
        String feedback
) {
}
