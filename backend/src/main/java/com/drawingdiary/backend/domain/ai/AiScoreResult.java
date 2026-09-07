package com.drawingdiary.backend.domain.ai;

/**
 * AI 서버 POST /api/ai/score의 응답. 스펙에는 relevanceScore·colorScore 두 개만 있는 줄
 * 알았는데 실제로는 feedback(평가 코멘트)까지 세 개가 필수로 내려온다.
 *
 * 좋아요 관련 값은 요청에도 응답에도 없다 — 좋아요 점수와 총점은 백엔드가 계산한다.
 *
 * <p><b>점수가 int가 아니라 Integer인 이유</b>: primitive로 두면 응답에 그 필드가 아예 없어도
 * Jackson이 조용히 0을 채운다. 그러면 AI 서버 스펙이 바뀌었을 때 "0점"이 정상 점수인 양
 * 저장되고 랭킹까지 오염된다. null로 받아야 누락을 감지해 INVALID_RESPONSE로 끊을 수 있다.
 */
public record AiScoreResult(
        Integer relevanceScore,
        Integer colorScore,
        String feedback
) {
}
