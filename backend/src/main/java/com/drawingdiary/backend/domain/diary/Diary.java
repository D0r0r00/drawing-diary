package com.drawingdiary.backend.domain.diary;

import com.drawingdiary.backend.domain.category.Category;
import com.drawingdiary.backend.domain.room.DrawingRoom;
import com.drawingdiary.backend.domain.theme.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private DrawingRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_guide_img_url", length = 500)
    private String aiGuideImgUrl;

    @Column(name = "final_img_url", length = 500)
    private String finalImgUrl;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "canvas_data")
    private byte[] canvasData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Diary(DrawingRoom room, Category category, Theme theme, String title, String content,
                 Visibility visibility) {
        this.room = room;
        this.category = category;
        this.theme = theme;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
    }

    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updateCanvasData(byte[] canvasData) {
        this.canvasData = canvasData;
    }

    public void applyFinalResult(String aiGuideImgUrl, String finalImgUrl) {
        this.aiGuideImgUrl = aiGuideImgUrl;
        this.finalImgUrl = finalImgUrl;
    }

    public void changeVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
}
