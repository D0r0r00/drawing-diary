package com.drawingdiary.backend.domain.notification;

import com.drawingdiary.backend.domain.notification.dto.NotificationReadAllResponse;
import com.drawingdiary.backend.domain.notification.dto.NotificationResponse;
import com.drawingdiary.backend.domain.notification.exception.NotNotificationReceiverException;
import com.drawingdiary.backend.domain.notification.exception.NotificationNotFoundException;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(Long userId) {
        return notificationRepository.findWithSenderByReceiverId(userId).stream()
                .map(notification -> {
                    User sender = notification.getSender();
                    return new NotificationResponse(
                            notification.getId(),
                            sender == null ? null : sender.getId(),
                            sender == null ? null : sender.getNickname(),
                            notification.getType(),
                            notification.getTargetId(),
                            notification.isRead(),
                            notification.getCreatedAt()
                    );
                })
                .toList();
    }

    /**
     * 이미 읽은 알림을 다시 읽어도 에러가 아니다 — 호출자가 원한 상태가 이미 됐을 뿐이라
     * 멱등하게 200으로 끝낸다. receiver는 LAZY 프록시지만 id만 읽으므로 추가 조회가 없다.
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getReceiver().getId().equals(userId)) {
            throw new NotNotificationReceiverException(notificationId);
        }

        notification.markAsRead();
    }

    @Transactional
    public NotificationReadAllResponse markAllAsRead(Long userId) {
        return new NotificationReadAllResponse(notificationRepository.markAllAsReadByReceiverId(userId));
    }

    /**
     * 알림을 남기는 유일한 진입점. 이벤트가 일어나는 서비스(Room/Follow/Comment/Like)가 호출한다.
     * <p>
     * 호출한 트랜잭션에 그대로 합류하므로, 팔로우나 댓글이 롤백되면 알림도 함께 사라진다.
     * 있지도 않은 일에 대한 알림이 남는 것보다 낫다는 판단이다. 반대로 알림 저장이 실패하면
     * 원래 동작까지 실패하는데, 지금은 FK가 보장된 id만 넘기므로 실패할 자리가 사실상 없다.
     * <p>
     * 받는 사람을 특정할 수 없거나(작성자가 모두 탈퇴한 일기) 보낸 사람과 같으면 조용히 건너뛴다 —
     * 호출자마다 이 검사를 반복하지 않도록 여기 한곳에 모았다.
     *
     * @param senderId null이면 시스템 알림
     */
    @Transactional
    public void notify(Long receiverId, Long senderId, NotificationType type, Long targetId) {
        if (receiverId == null || receiverId.equals(senderId)) {
            return;
        }

        // 호출자가 이미 검증했거나 조회 결과로 얻은 id라 존재가 보장된다. getReferenceById는
        // 프록시만 만들고 SELECT를 날리지 않으므로 알림 하나당 INSERT 한 번으로 끝난다.
        notificationRepository.save(Notification.builder()
                .receiver(userRepository.getReferenceById(receiverId))
                .sender(senderId == null ? null : userRepository.getReferenceById(senderId))
                .type(type)
                .targetId(targetId)
                .build());
    }
}
