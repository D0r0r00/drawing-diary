package com.drawingdiary.backend.domain.notification;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    /**
     * 시스템 알림 자리를 남겨두려고 nullable이다. 지금은 네 이벤트 모두 사람이 보내지만,
     * 조회 시 탈퇴한 회원도 여기서 null로 떨어진다(left join + User의 @SQLRestriction).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /**
     * FOLLOW/COMMENT/LIKE/ROOM_INVITE 각각 다른 엔티티(diary_id, room_id 등)를 가리키므로 FK로 고정하지 않는다.
     * FOLLOW는 sender 자체가 이동할 대상이라 따로 가리킬 것이 없어 null이다.
     */
    @Column(name = "target_id")
    private Long targetId;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(User receiver, User sender, NotificationType type, Long targetId) {
        this.receiver = receiver;
        this.sender = sender;
        this.type = type;
        this.targetId = targetId;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
