package com.drawingdiary.backend.domain.room;

import com.drawingdiary.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    long deleteByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * Selecting the User entity rather than the RoomMember row lets @SQLRestriction
     * on User filter out soft-deleted accounts, so a withdrawn member disappears
     * from the room roster — and from the collaborator copy made at submit — without
     * needing to clean up room_members rows.
     *
     * Ordered by membership id so the owner, added when the room was created, is
     * always first.
     */
    @Query("select m.user from RoomMember m where m.room.id = :roomId order by m.id asc")
    List<User> findMembersByRoomId(@Param("roomId") Long roomId);
}
