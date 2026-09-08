package com.drawingdiary.backend.domain.room;

import com.drawingdiary.backend.domain.category.Category;
import com.drawingdiary.backend.domain.category.CategoryRepository;
import com.drawingdiary.backend.domain.category.exception.CategoryNotFoundException;
import com.drawingdiary.backend.domain.diary.Diary;
import com.drawingdiary.backend.domain.diary.DiaryCollaborator;
import com.drawingdiary.backend.domain.diary.DiaryCollaboratorRepository;
import com.drawingdiary.backend.domain.diary.DiaryRepository;
import com.drawingdiary.backend.domain.notification.NotificationService;
import com.drawingdiary.backend.domain.notification.NotificationType;
import com.drawingdiary.backend.domain.room.dto.RoomCanvasResponse;
import com.drawingdiary.backend.domain.room.dto.RoomCanvasSaveRequest;
import com.drawingdiary.backend.domain.room.dto.RoomCanvasSaveResponse;
import com.drawingdiary.backend.domain.room.dto.RoomCreateResponse;
import com.drawingdiary.backend.domain.room.dto.RoomInviteRequest;
import com.drawingdiary.backend.domain.room.dto.RoomMemberResponse;
import com.drawingdiary.backend.domain.room.dto.RoomResponse;
import com.drawingdiary.backend.domain.room.dto.RoomSubmitRequest;
import com.drawingdiary.backend.domain.room.dto.RoomSubmitResponse;
import com.drawingdiary.backend.domain.room.exception.InvalidCanvasDataException;
import com.drawingdiary.backend.domain.room.exception.NotRoomMemberException;
import com.drawingdiary.backend.domain.room.exception.NotRoomOwnerException;
import com.drawingdiary.backend.domain.room.exception.OwnerCannotLeaveRoomException;
import com.drawingdiary.backend.domain.room.exception.RoomAlreadyFinishedException;
import com.drawingdiary.backend.domain.room.exception.RoomInviteNotFoundException;
import com.drawingdiary.backend.domain.room.exception.RoomNotFoundException;
import com.drawingdiary.backend.domain.room.exception.RoomSubmitContentMissingException;
import com.drawingdiary.backend.domain.tag.TagService;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
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
    private final NotificationService notificationService;
    private final TagService tagService;

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

            // 위 continue들을 통과한 경우에만 — 이미 멤버이거나 초대가 살아 있으면 초대장이
            // 새로 생기지 않으므로 알림도 다시 보내지 않는다.
            notificationService.notify(invitedUserId, userId, NotificationType.ROOM_INVITE, roomId);
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

        // 본문에 담겨 온 값이 우선이고, 없으면 방에 임시 저장해둔 값을 쓴다. 그림을 그리는
        // 동안 자동 저장만 하다가 발행 시에는 공개 범위만 보내는 흐름을 지원하기 위한 것이다.
        String title = firstNonNull(request.title(), room.getTitle());
        String content = firstNonNull(request.content(), room.getContent());
        byte[] canvasData = request.canvasData() == null
                ? room.getCanvasData()
                : decodeCanvasData(request.canvasData());

        // diaries.title/content는 NOT NULL이라 둘 다 비어 있으면 여기서 막아야 한다.
        // 그냥 두면 제약 위반이 500으로 새어나간다.
        if (!StringUtils.hasText(title)) {
            throw new RoomSubmitContentMissingException("제목");
        }
        if (!StringUtils.hasText(content)) {
            throw new RoomSubmitContentMissingException("내용");
        }

        Diary diary = diaryRepository.save(Diary.builder()
                .room(room)
                .category(findCategoryOrThrow(request.categoryId()))
                .title(title)
                .content(content)
                .finalImgUrl(request.finalImg())
                .visibility(request.visibility())
                .build());

        diary.updateCanvasData(canvasData);

        List<DiaryCollaborator> collaborators = roomMemberRepository.findMembersByRoomId(roomId).stream()
                .map(member -> DiaryCollaborator.builder()
                        .diary(diary)
                        .user(member)
                        .build())
                .toList();
        diaryCollaboratorRepository.saveAll(collaborators);

        // 태그는 이름으로 받아 없으면 그때 만든다. 수정 경로(PATCH /api/diaries/{id})와 같은
        // TagService를 타므로 정규화·상한 규칙이 발행과 수정에서 어긋나지 않는다.
        tagService.replaceTags(diary, request.tags());

        room.changeStatus(RoomStatus.FINISHED);

        return new RoomSubmitResponse(diary.getId());
    }

    /**
     * 작업 중간 저장. 방 멤버면 누구나 저장할 수 있어서, 여럿이 같이 그리다 아무나 나가도
     * 마지막 상태가 남는다. 마지막에 저장한 사람의 내용이 남는 방식이라 동시 편집의 병합은
     * 하지 않는다 — 실시간 동기화가 붙기 전까지의 임시 보관 용도다.
     *
     * 이미 발행된 방을 막는 이유는 발행 시점의 스냅샷이 곧 일기이기 때문이다. 발행 후에도
     * 방의 캔버스를 고칠 수 있으면 일기와 방의 내용이 소리 없이 어긋난다.
     */
    @Transactional
    public RoomCanvasSaveResponse saveCanvas(Long userId, Long roomId, RoomCanvasSaveRequest request) {
        DrawingRoom room = getRoomOrThrow(roomId);
        requireMember(roomId, userId);

        if (room.getStatus() == RoomStatus.FINISHED) {
            throw new RoomAlreadyFinishedException(roomId);
        }

        room.saveWorkInProgress(
                request.canvasData() == null ? null : decodeCanvasData(request.canvasData()),
                request.title(),
                request.content());

        // @UpdateTimestamp는 flush 때 채워지므로, 응답에 방금 저장한 시각을 담으려면
        // 커밋을 기다리지 않고 여기서 flush해야 한다.
        roomRepository.flush();

        return new RoomCanvasSaveResponse(room.getId(), room.getUpdatedAt());
    }

    /**
     * 저장해둔 것이 없으면 세 필드가 모두 null로 내려간다 — 아직 아무도 그리지 않은 방과
     * 방금 만든 방을 클라이언트가 같은 방식으로 다룰 수 있게 하려는 것이다.
     */
    @Transactional(readOnly = true)
    public RoomCanvasResponse findCanvas(Long userId, Long roomId) {
        DrawingRoom room = getRoomOrThrow(roomId);
        requireMember(roomId, userId);

        return new RoomCanvasResponse(
                room.getId(),
                encodeCanvasData(room.getCanvasData()),
                room.getTitle(),
                room.getContent(),
                room.getUpdatedAt());
    }

    /**
     * BYTEA를 JSON에 실을 수 없어 Base64로 주고받는다. Diary 상세 조회의 canvasData와
     * 같은 인코딩이라 프론트는 한 가지 방식만 다루면 된다.
     */
    private byte[] decodeCanvasData(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new InvalidCanvasDataException();
        }
    }

    private String encodeCanvasData(byte[] canvasData) {
        if (canvasData == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(canvasData);
    }

    private String firstNonNull(String requestValue, String savedValue) {
        return requestValue != null ? requestValue : savedValue;
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

    /**
     * AI 선화 가이드처럼 "아직 작업 중인 방에서, 멤버가" 하는 일에 쓰는 진입점.
     * 임시 저장(saveCanvas)과 같은 조건이라 판정을 한 곳에 모아둔다 — 한쪽만 고치면
     * 발행이 끝난 방에 가이드만 계속 생성되는 식으로 어긋난다.
     */
    @Transactional(readOnly = true)
    public void requireEditableMember(Long userId, Long roomId) {
        DrawingRoom room = getRoomOrThrow(roomId);
        requireMember(roomId, userId);

        if (room.getStatus() == RoomStatus.FINISHED) {
            throw new RoomAlreadyFinishedException(roomId);
        }
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
