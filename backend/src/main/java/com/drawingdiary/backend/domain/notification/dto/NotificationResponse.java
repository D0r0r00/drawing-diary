package com.drawingdiary.backend.domain.notification.dto;

import com.drawingdiary.backend.domain.notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        Long senderId,
        String senderNickname,
        NotificationType type,
        Long targetId,
        boolean isRead,
        LocalDateTime createdAt
) {
}
