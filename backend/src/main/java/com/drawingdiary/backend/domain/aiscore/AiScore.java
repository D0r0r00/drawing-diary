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
}
