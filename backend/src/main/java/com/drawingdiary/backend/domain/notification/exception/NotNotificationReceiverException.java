package com.drawingdiary.backend.domain.notification.exception;

public class NotNotificationReceiverException extends RuntimeException {

    public NotNotificationReceiverException(Long notificationId) {
        super("본인에게 온 알림만 처리할 수 있습니다: " + notificationId);
    }
}
