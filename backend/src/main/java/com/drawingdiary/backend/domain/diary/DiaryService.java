package com.drawingdiary.backend.domain.diary;

import com.drawingdiary.backend.domain.aiscore.AiScoreRepository;
import com.drawingdiary.backend.domain.comment.CommentRepository;
import com.drawingdiary.backend.domain.diary.dto.DiaryDeleteResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryDetailResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryListResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryUpdateRequest;
import com.drawingdiary.backend.domain.diary.dto.DiaryUpdateResponse;
import com.drawingdiary.backend.domain.diary.dto.MyDiaryResponse;
import com.drawingdiary.backend.domain.diary.exception.DiaryAccessDeniedException;
import com.drawingdiary.backend.domain.diary.exception.DiaryNotFoundException;
import com.drawingdiary.backend.domain.diary.exception.NotDiaryCollaboratorException;
import com.drawingdiary.backend.domain.follow.FollowRepository;
import com.drawingdiary.backend.domain.like.LikeRepository;
import com.drawingdiary.backend.domain.tag.DiaryTagRepository;
import com.drawingdiary.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryCollaboratorRepository diaryCollaboratorRepository;
    private final FollowRepository followRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final AiScoreRepository aiScoreRepository;
    private final DiaryTagRepository diaryTagRepository;

    /**
     * 둘러보기용 목록이라 PUBLIC만 내려간다. 작성자는 일기별로 다시 조회하지 않고
     * 협업자를 한 번에 가져와 메모리에서 묶는다(총 2쿼리).
     */
    @Transactional(readOnly = true)
    public List<DiaryListResponse> findAll() {
        List<Diary> diaries = diaryRepository.findByVisibilityOrderByIdDesc(Visibility.PUBLIC);
        Map<Long, User> authors = findAuthors(diaries.stream().map(Diary::getId).toList());

        return diaries.stream()
                .map(diary -> {
                    User author = authors.get(diary.getId());
                    return new DiaryListResponse(
                            diary.getId(),
                            diary.getTitle(),
                            author == null ? null : author.getId(),
                            author == null ? null : author.getNickname(),
                            diary.getCreatedAt(),
                            diary.getFinalImgUrl(),
                            diary.getVisibility()
                    );
                })
                .toList();
    }

    /**
     * 본인이 참여한 일기이므로 visibility와 무관하게 전부 보인다.
     */
    @Transactional(readOnly = true)
    public List<MyDiaryResponse> findMine(Long userId) {
        return diaryCollaboratorRepository.findDiariesByUserId(userId).stream()
                .map(diary -> new MyDiaryResponse(
                        diary.getId(),
                        diary.getTitle(),
                        diary.getCreatedAt(),
                        diary.getFinalImgUrl(),
                        diary.getVisibility()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaryDetailResponse find(Long userId, Long diaryId) {
        Diary diary = getDiaryOrThrow(diaryId);

        if (!canRead(diary, userId)) {
            throw new DiaryAccessDeniedException(diaryId);
        }

        return new DiaryDetailResponse(
                diary.getId(),
                diary.getTitle(),
                diary.getContent(),
                encodeCanvasData(diary.getCanvasData()),
                diary.getFinalImgUrl(),
                diary.getVisibility(),
                diary.getCreatedAt()
        );
    }

    @Transactional
    public DiaryUpdateResponse update(Long userId, Long diaryId, DiaryUpdateRequest request) {
        Diary diary = getDiaryOrThrow(diaryId);
        requireCollaborator(diaryId, userId);

        diary.applyUpdate(request.title(), request.textContent(), request.visibility());

        // @UpdateTimestamp는 flush 시점에 채워지므로, 응답에 갱신된 값을 담으려면
        // 트랜잭션 커밋을 기다리지 않고 여기서 flush해야 한다.
        diaryRepository.flush();

        return new DiaryUpdateResponse(diary.getId(), diary.getTitle(), diary.getUpdatedAt());
    }

    /**
     * diaries에는 deleted_at 컬럼이 없어 하드 삭제한다. diary_id를 참조하는 다섯 테이블에
     * ON DELETE CASCADE가 걸려 있지 않으므로, 자식 행을 먼저 지우지 않으면 FK 위반으로 실패한다.
     */
    @Transactional
    public DiaryDeleteResponse delete(Long userId, Long diaryId) {
        Diary diary = getDiaryOrThrow(diaryId);
        requireCollaborator(diaryId, userId);

        commentRepository.deleteByDiaryId(diaryId);
        likeRepository.deleteByDiaryId(diaryId);
        aiScoreRepository.deleteByDiaryId(diaryId);
        diaryTagRepository.deleteByDiaryId(diaryId);
        diaryCollaboratorRepository.deleteByDiaryId(diaryId);
        diaryRepository.delete(diary);

        return new DiaryDeleteResponse("일기가 삭제되었습니다");
    }

    /**
     * 공개 범위별 조회 권한.
     * <ul>
     *   <li>PUBLIC — 로그인한 누구나</li>
     *   <li>FOLLOWERS_ONLY — 협업자이거나, 작성자를 팔로우하는 사람</li>
     *   <li>PRIVATE — 협업자만</li>
     * </ul>
     * 협업자 여부를 팔로우보다 먼저 보므로, 협업자는 작성자를 팔로우하지 않아도(그리고
     * 본인이 작성자여서 팔로우가 불가능해도) 항상 볼 수 있다.
     */
    private boolean canRead(Diary diary, Long userId) {
        if (diary.getVisibility() == Visibility.PUBLIC) {
            return true;
        }
        if (isCollaborator(diary.getId(), userId)) {
            return true;
        }
        if (diary.getVisibility() != Visibility.FOLLOWERS_ONLY) {
            return false;
        }

        // 작성자가 남아있지 않으면 팔로우를 확인할 대상 자체가 없으므로 막는다.
        User author = findAuthor(diary.getId());
        return author != null
                && followRepository.existsByFollowerIdAndFollowingId(userId, author.getId());
    }

    private User findAuthor(Long diaryId) {
        return findAuthors(List.of(diaryId)).get(diaryId);
    }

    /**
     * 작성자는 협업자 중 가장 먼저 등록된 사람 — 발행 시 방 멤버를 참여 순으로 복사하므로
     * 곧 방장이다. 탈퇴한 계정은 조회 단계에서 걸러지므로 그 다음 생존자가 작성자가 되고,
     * 전원이 탈퇴했다면 작성자 없이(null) 내려간다.
     */
    private Map<Long, User> findAuthors(List<Long> diaryIds) {
        if (diaryIds.isEmpty()) {
            return Map.of();
        }

        return diaryCollaboratorRepository.findWithUserByDiaryIds(diaryIds).stream()
                .collect(Collectors.toMap(
                        collaborator -> collaborator.getDiary().getId(),
                        DiaryCollaborator::getUser,
                        (first, next) -> first
                ));
    }

    /**
     * BYTEA라 JSON에 그대로 담을 수 없어 Base64로 인코딩한다.
     */
    private String encodeCanvasData(byte[] canvasData) {
        if (canvasData == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(canvasData);
    }

    private Diary getDiaryOrThrow(Long diaryId) {
        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> new DiaryNotFoundException(diaryId));
    }

    private boolean isCollaborator(Long diaryId, Long userId) {
        return diaryCollaboratorRepository.existsByDiaryIdAndUserId(diaryId, userId);
    }

    private void requireCollaborator(Long diaryId, Long userId) {
        if (!isCollaborator(diaryId, userId)) {
            throw new NotDiaryCollaboratorException(diaryId);
        }
    }
}
