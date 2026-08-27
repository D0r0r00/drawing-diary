package com.drawingdiary.backend.domain.notification;

import com.drawingdiary.backend.domain.notification.dto.NotificationReadAllResponse;
import com.drawingdiary.backend.domain.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> findMine(Authentication authentication) {
        return ResponseEntity.ok(notificationService.findMine(currentUserId(authentication)));
    }

    /**
     * "read-all"은 {notificationId} 자리와 경로 길이가 달라서 서로 가리지 않는다.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<NotificationReadAllResponse> markAllAsRead(Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAllAsRead(currentUserId(authentication)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(Authentication authentication, @PathVariable Long notificationId) {
        notificationService.markAsRead(currentUserId(authentication), notificationId);
        return ResponseEntity.ok().build();
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
