package com.drawingdiary.backend.domain.room;

import com.drawingdiary.backend.domain.user.User;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "drawing_rooms")
@SQLDelete(sql = "UPDATE drawing_rooms SET deleted_at = CURRENT_TIMESTAMP WHERE room_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrawingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    /**
     * 발행 전 작업 중인 그림. 방을 나갔다 들어와도 이어 그릴 수 있도록 여기에 임시 보관하고,
     * 발행 시점에 Diary로 옮겨진다. 발행 후에도 지우지 않아서 방을 열면 마지막 상태가 남아 있다.
     */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "canvas_data")
    private byte[] canvasData;

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    public DrawingRoom(User owner, RoomStatus status) {
        this.owner = owner;
        this.status = status;
    }

    public void changeStatus(RoomStatus status) {
        this.status = status;
    }

    /**
     * 작업 중간 저장. null인 인자는 "이번에 보내지 않았다"는 뜻이라 기존 값을 유지한다.
     * Diary.applyUpdate와 같은 규칙이라, 캔버스만 자동 저장하면서 제목은 건드리지 않는
     * 식의 부분 저장이 가능하다.
     */
    public void saveWorkInProgress(byte[] canvasData, String title, String content) {
        if (canvasData != null) {
            this.canvasData = canvasData;
        }
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }
}
