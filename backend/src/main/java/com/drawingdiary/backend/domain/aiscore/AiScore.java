package com.drawingdiary.backend.domain.aiscore;

import com.drawingdiary.backend.domain.diary.Diary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false, unique = true)
    private Diary diary;

    @Column(nullable = false)
    private Integer totalScore;

    @Column(nullable = false)
    private Integer colorScore;

    /**
     * 통합 랭킹으로 바뀌면서 더 이상 쓰지 않는다. 컬럼이 NOT NULL이고 기존 행이 있어 지울 수
     * 없으므로 항상 0으로 채운다. 컬럼을 실제로 없애려면 별도 마이그레이션이 필요하다.
     *
     * @deprecated 점수 계산에 들어가지 않는다. 읽지 말 것.
     */
    @Deprecated
    @Column(nullable = false)
    private Integer themeScore;

    @Column(nullable = false)
    private Integer relevanceScore;

    @Column(columnDefinition = "TEXT")
    private String aiComment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiScore(Diary diary, Integer totalScore, Integer colorScore, Integer themeScore,
                   Integer relevanceScore, String aiComment) {
        this.diary = diary;
        this.totalScore = totalScore;
        this.colorScore = colorScore;
        this.themeScore = themeScore;
        this.relevanceScore = relevanceScore;
        this.aiComment = aiComment;
    }

    /**
     * AI 점수를 받아 저장할 때. 좋아요 점수는 저장 시점의 좋아요 수로 계산해 넘어온다.
     */
    public void applyAiScores(int relevanceScore, int colorScore, int likeScore) {
        this.relevanceScore = relevanceScore;
        this.colorScore = colorScore;
        this.totalScore = ScoreCalculator.totalScore(relevanceScore, colorScore, likeScore);
    }

    /**
     * 좋아요만 변했을 때. AI 점수는 그대로 두고 총점만 다시 계산한다.
     */
    public void applyLikeScore(int likeScore) {
        this.totalScore = ScoreCalculator.totalScore(this.relevanceScore, this.colorScore, likeScore);
    }
}
