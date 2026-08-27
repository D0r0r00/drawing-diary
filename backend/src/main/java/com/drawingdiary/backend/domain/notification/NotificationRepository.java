package com.drawingdiary.backend.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * senderNickname을 담아야 하는데 sender가 LAZY라 그냥 조회하면 알림 수만큼 쿼리가 더 나간다.
     * <p>
     * inner가 아니라 <b>left</b> join fetch인 이유는 두 가지다. sender가 없는 시스템 알림이
     * 사라지면 안 되고, 탈퇴한 회원이 보낸 알림도 목록에서 통째로 빠지는 대신 sender만 null로
     * 떨어져야 한다(User의 @SQLRestriction이 on 절에 붙는다). 댓글 목록과는 반대 선택인데,
     * 댓글은 내용이 남의 것이라 지우는 게 맞지만 알림은 내가 받은 기록이라 남아야 하기 때문이다.
     */
    @Query("select n from Notification n left join fetch n.sender where n.receiver.id = :userId order by n.id desc")
    List<Notification> findWithSenderByReceiverId(@Param("userId") Long userId);

    /**
     * 건건이 엔티티를 올려 dirty checking에 맡기면 안 읽은 알림 수만큼 UPDATE가 나간다.
     * 벌크 UPDATE 한 번으로 끝내고, 반환값을 그대로 updatedCount로 쓴다.
     * 이미 읽은 알림은 조건에서 빠지므로 다시 호출하면 0이 나온다.
     */
    @Modifying
    @Query("update Notification n set n.isRead = true where n.receiver.id = :userId and n.isRead = false")
    int markAllAsReadByReceiverId(@Param("userId") Long userId);
}
