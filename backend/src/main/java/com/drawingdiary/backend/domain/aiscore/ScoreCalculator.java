package com.drawingdiary.backend.domain.aiscore;

/**
 * 팀에서 확정한 랭킹 점수 공식. 점수 저장(AiScoreService)과 좋아요 변동(LikeService)이
 * 같은 계산을 써야 total_score가 어긋나지 않으므로 한 곳에 모아둔다.
 *
 * <pre>
 * likeScore  = min(좋아요 수 × 5, 100)
 * totalScore = relevanceScore × 0.5 + colorScore × 0.3 + likeScore × 0.2
 * </pre>
 */
public final class ScoreCalculator {

    static final int MIN_SCORE = 0;

    static final int MAX_SCORE = 100;

    private static final int POINTS_PER_LIKE = 5;

    private ScoreCalculator() {
    }

    /**
     * 좋아요 20개에서 100점 상한에 닿고, 그 뒤로는 아무리 더 받아도 오르지 않는다.
     */
    public static int likeScore(long likeCount) {
        long raw = likeCount * POINTS_PER_LIKE;
        return (int) Math.min(raw, MAX_SCORE);
    }

    /**
     * 세 점수 모두 0~100이라 가중합도 0~100 안에 들어온다.
     *
     * 가중치가 0.5/0.3/0.2라 결과가 소수로 떨어지는데(예: 81×0.5 = 40.5), total_score 컬럼이
     * integer라 어딘가에서는 잘라야 한다. double로 합산한 뒤 한 번만 반올림하는 이유는
     * 항마다 잘라 버리면 오차가 최대 3점까지 쌓여 순위가 뒤집힐 수 있기 때문이다.
     */
    public static int totalScore(int relevanceScore, int colorScore, int likeScore) {
        double weighted = relevanceScore * 0.5 + colorScore * 0.3 + likeScore * 0.2;
        return (int) Math.round(weighted);
    }
}
