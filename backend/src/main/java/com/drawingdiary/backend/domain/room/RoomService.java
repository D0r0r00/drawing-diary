package com.drawingdiary.backend.domain.room;

import com.drawingdiary.backend.domain.category.Category;
import com.drawingdiary.backend.domain.category.CategoryRepository;
import com.drawingdiary.backend.domain.category.exception.CategoryNotFoundException;
import com.drawingdiary.backend.domain.diary.Diary;
import com.drawingdiary.backend.domain.diary.DiaryCollaborator;
import com.drawingdiary.backend.domain.diary.DiaryCollaboratorRepository;
import com.drawingdiary.backend.domain.diary.DiaryRepository;
import com.drawingdiary.backend.domain.room.dto.RoomCreateResponse;
import com.drawingdiary.backend.domain.room.dto.RoomInviteRequest;
import com.drawingdiary.backend.domain.room.dto.RoomMemberResponse;
import com.drawingdiary.backend.domain.room.dto.RoomResponse;
import com.drawingdiary.backend.domain.room.dto.RoomSubmitRequest;
import com.drawingdiary.backend.domain.room.dto.RoomSubmitResponse;
import com.drawingdiary.backend.domain.room.exception.NotRoomMemberException;
import com.drawingdiary.backend.domain.room.exception.NotRoomOwnerException;
import com.drawingdiary.backend.domain.room.exception.OwnerCannotLeaveRoomException;
import com.drawingdiary.backend.domain.room.exception.RoomAlreadyFinishedException;
import com.drawingdiary.backend.domain.room.exception.RoomInviteNotFoundException;
import com.drawingdiary.backend.domain.room.exception.RoomNotFoundException;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final DrawingRoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomInviteRepository roomInviteRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryCollaboratorRepository diaryCollaboratorRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * The owner is written into room_members as well as drawing_rooms.owner_id so
     * that every membership check — read, invite, submit — is a single lookup
     * against one table, instead of "member OR owner" everywhere.
     */
    @Transactional
    public RoomCreateResponse create(Long userId) {
        User owner = getUserOrThrow(userId);

        DrawingRoom room = roomRepository.save(DrawingRoom.builder()
                .owner(owner)
                .status(RoomStatus.WAITING)
                .build());

        roomMemberRepository.save(RoomMember.builder()
                .room(room)
                .user(owner)
                .build());

        return new RoomCreateResponse(room.getId());
    }

    @Transactional(readOnly = true)
    public RoomResponse find(Long userId, Long roomId) {
        DrawingRoom room = getRoomOrThrow(roomId);
        requireMember(roomId, userId);

        List<RoomMemberResponse> members = roomMemberRepository.findMembersByRoomId(roomId).stream()
                .map(member -> new RoomMemberResponse(member.getId(), member.getNickname(), member.getProfileImageUrl()))
                .toList();

        return new RoomResponse(room.getId(), room.getStatus(), room.getOwner().getId(), members);
    }

    /**
     * Soft delete only: @SQLDelete on DrawingRoom turns this into an UPDATE of
     * deleted_at, so room_members and any diary already published from the room
     * keep their foreign keys intact.
     */
    @Transactional
    public void delete(Long userId, Long roomId) {
        DrawingRoom room = getRoomOrThrow(roomId);
        requireOwner(room, userId);

        roomRepository.delete(room);
    }

    /**
     * Skips rather than rejects users who are already members or already hold a
     * PENDING invite: the client invites a batch picked from a friend list, and
     * failing the whole call because one entry is stale would be worse than
     * quietly converging on the intended state.
     */
    @Transactional
    public void invite(Long userId, Long roomId, RoomInviteRequest request) {
        DrawingRoom room = getRoomOrThrow(roomId);
        requireMember(roomId, userId);

        User sender = getUserOrThrow(userId);

        for (Long invitedUserId : request.invitedUserIds()) {
            User receiver = getUserOrThrow(invitedUserId);

            if (roomMemberRepository.existsByRoomIdAndUserId(roomId, invitedUserId)) {
                continue;
            }
            if (roomInviteRepository.existsByRoomIdAndReceiverIdAndStatus(roomId, invitedUserId, InviteStatus.PENDING)) {
                continue;
            }

            roomInviteRepository.save(RoomInvite.builder()
                    .room(room)
                    .sender(sender)
                    .receiver(receiver)
                    .status(InviteStatus.PENDING)
                    .build());
        }
    }

    /**
     * Membership is checked before the invite is, so a re-sent join from someone
     * already in the room is a no-op rather than a 403 — the caller is already in
     * the state they asked for, and their invite has by then been consumed.
     */
    @Transactional
    public void join(Long userId, Long roomId) {
        DrawingRoom room = getRoomOrThrow(roomId);

        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            return;
        }

        RoomInvite invite = roomInviteRepository
                .findByRoomIdAndReceiverIdAndStatus(roomId, userId, InviteStatus.PENDING)
                .orElseThrow(() -> new RoomInviteNotFoundException(roomId));

        roomMemberRepository.save(RoomMember.builder()
                .room(room)
                .user(getUserOrThrow(userId))
                .build());

        invite.changeStatus(InviteStatus.ACCEPT);
    }

    /**
     * The owner is refused rather than silently handed off, because a room whose
     * owner_id points at a non-member would leave delete and submit without a
     * responsible party. The owner deletes the room instead.
     */
    @Transactional
    public void leave(Long userId, Long roomId) {
        DrawingRoom room = getRoomOrThrow(roomId);

        if (room.getOwner().getId().equals(userId)) {
            throw new OwnerCannotLeaveRoomException(roomId);
        }

        roomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    /**
     * The publish step: a Diary cannot exist without the room it was drawn in, so
     * this is the only path that creates one. The member list is snapshotted into
     * diary_collaborators here — later joins or leaves must not rewrite the
     * authorship of an already-published diary.
     */
    @Transactional
    public RoomSubmitResponse submit(Long userId, Long roomId, RoomSubmitRequest request) {
        DrawingRoom room = getRoomOrThrow(roomId);
        requireMember(roomId, userId);

        if (room.getStatus() == RoomStatus.FINISHED) {
            throw new RoomAlreadyFinishedException(roomId);
        }

        Diary diary = diaryRepository.save(Diary.builder()
                .room(room)
                .category(findCategoryOrThrow(request.categoryId()))
                .title(request.title())
                .content(request.content())
                .finalImgUrl(request.finalImg())
                .visibility(request.visibility())
                .build());

        List<DiaryCollaborator> collaborators = roomMemberRepository.findMembersByRoomId(roomId).stream()
                .map(member -> DiaryCollaborator.builder()
                        .diary(diary)
                        .user(member)
                        .build())
                .toList();
        diaryCollaboratorRepository.saveAll(collaborators);

        room.changeStatus(RoomStatus.FINISHED);

        return new RoomSubmitResponse(diary.getId());
    }

    private Category findCategoryOrThrow(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private DrawingRoom getRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void requireMember(Long roomId, Long userId) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new NotRoomMemberException(roomId);
        }
    }

    private void requireOwner(DrawingRoom room, Long userId) {
        if (!room.getOwner().getId().equals(userId)) {
            throw new NotRoomOwnerException(room.getId());
        }
    }
}
