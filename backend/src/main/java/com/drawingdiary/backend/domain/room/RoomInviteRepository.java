package com.drawingdiary.backend.domain.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomInviteRepository extends JpaRepository<RoomInvite, Long> {

    boolean existsByRoomIdAndReceiverIdAndStatus(Long roomId, Long receiverId, InviteStatus status);

    Optional<RoomInvite> findByRoomIdAndReceiverIdAndStatus(Long roomId, Long receiverId, InviteStatus status);
}
