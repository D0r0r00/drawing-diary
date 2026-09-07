package com.drawingdiary.backend.domain.diary;

import com.drawingdiary.backend.domain.aiscore.AiScoreRepository;
import com.drawingdiary.backend.domain.category.Category;
import com.drawingdiary.backend.domain.comment.CommentRepository;
import com.drawingdiary.backend.domain.diary.dto.DiaryDeleteResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryDetailResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryListResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryUpdateRequest;
import com.drawingdiary.backend.domain.diary.dto.DiaryUpdateResponse;
import com.drawingdiary.backend.domain.diary.dto.FeedItemResponse;
import com.drawingdiary.backend.domain.diary.dto.FeedUserResponse;
import com.drawingdiary.backend.domain.diary.dto.MyDiaryResponse;
import com.drawingdiary.backend.domain.diary.exception.DiaryAccessDeniedException;
import com.drawingdiary.backend.domain.diary.exception.DiaryNotFoundException;
import com.drawingdiary.backend.domain.diary.exception.NotDiaryCollaboratorException;
import com.drawingdiary.backend.domain.follow.FollowRepository;
import com.drawingdiary.backend.domain.like.LikeRepository;
import com.drawingdiary.backend.domain.tag.DiaryTagRepository;
import com.drawingdiary.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * 첫 페이지는 "가장 큰 id보다 작은 것"이므로 커서 없이 들어온 요청에 이 값을 쓴다.
     */
    private static final long FIRST_PAGE_CURSOR = Long.MAX_VALUE;

    private static final int DEFAULT_LIMIT = 10;

    /**
     * limit을 그대로 믿으면 한 번의 요청으로 테이블 전체를 긁어갈 수 있어 상한을 둔다.
     */
    private static final int MAX_LIMIT = 50;

    /**
     * 둘러보기용 목록이라 PUBLIC만 내려간다. 작성자는 일기별로 다시 조회하지 않고
     * 협업자를 한 번에 가져와 메모리에서 묶는다(총 2쿼리).
     *
     * @deprecated /api/explore로 대체됐다. 페이지네이션이 없어 일기가 늘어나면 응답이 계속
     * 커지므로 새 화면에서는 쓰지 말 것. 이미 붙어 있는 프론트를 깨지 않으려고 남겨둔다.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<DiaryListResponse> findAll() {
        List<Diary> diaries = diaryRepository.findByVisibilityBefore(
                Visibility.PUBLIC, FIRST_PAGE_CURSOR, Pageable.unpaged());
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
     * 탐색 피드 — 팔로우 여부와 무관하게 PUBLIC 전체를 최신순으로. deprecated된 findAll과
     * 같은 조회를 쓰고 페이지네이션만 얹은 것이라 두 경로의 결과가 항상 일치한다.
     */
    @Transactional(readOnly = true)
    public List<FeedItemResponse> findExplore(Long cursor, int limit) {
        return toFeedItems(diaryRepository.findByVisibilityBefore(
                Visibility.PUBLIC, cursorOrFirstPage(cursor), PageRequest.ofSize(pageSize(limit))));
    }

    /**
     * 팔로잉 피드 — 내가 팔로우하는 사람이 <b>협업자로 참여한</b> 일기. 방장이 아니어도 되고,
     * 한 일기에서 여러 명을 팔로우 중이어도 한 번만 나온다. 팔로우는 단방향이고 자기 자신은
     * 팔로우할 수 없으므로 내 일기는 여기 나오지 않는다(내 일기는 /api/diaries/my).
     *
     * 응답의 user는 여전히 작성자(첫 생존 협업자)다 — 목록·상세·알림이 모두 findAuthors를
     * 쓰므로 "화면에 보이는 작성자"가 어디서나 같은 사람이다. 팔로우한 사람이 방장이 아니어서
     * 카드에 낯선 이름이 뜰 수는 있지만, 일기 하나의 대표 작성자는 하나여야 하고 그 자리를
     * "내가 팔로우한 협업자"로 바꾸면 보는 사람마다 작성자가 달라진다.
     *
     * 공개 범위 판정은 DiaryRepository.findFeedByAuthorIds가 SQL로 한 번에 처리한다.
     * 일기마다 canRead를 부르면 팔로우·협업자 확인이 건수만큼 반복될 자리다.
     */
    @Transactional(readOnly = true)
    public List<FeedItemResponse> findFeed(Long userId, Long cursor, int limit) {
        List<Long> authorIds = followRepository.findFollowingsByFollowerId(userId).stream()
                .map(User::getId)
                .toList();

        // 빈 IN 절은 DB마다 처리가 갈리는데, 어차피 결과가 없는 게 확실하므로 쿼리 자체를 건너뛴다.
        if (authorIds.isEmpty()) {
            return List.of();
        }

        return toFeedItems(diaryRepository.findFeedByAuthorIds(
                userId, authorIds, cursorOrFirstPage(cursor), PageRequest.ofSize(pageSize(limit))));
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
                        diary.getContent(),
                        diary.getFinalImgUrl(),
                        diary.getCreatedAt(),
                        categoryId(diary),
                        categoryName(diary),
                        diary.getVisibility()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaryDetailResponse find(Long userId, Long diaryId) {
        Diary diary = getReadableDiaryOrThrow(userId, diaryId);

        return new DiaryDetailResponse(
                diary.getId(),
                diary.getTitle(),
                diary.getContent(),
                diary.getFinalImgUrl(),
                diary.getFinalImgUrl(),
                diary.getCreatedAt(),
                categoryId(diary),
                categoryName(diary),
                diary.getVisibility(),
                encodeCanvasData(diary.getCanvasData()),
                diary.getContent()
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
     * 댓글·좋아요처럼 "일기를 볼 수 있는 사람만" 허용해야 하는 다른 도메인이 같은 판정을 쓰도록
     * 열어둔 진입점. 조회 권한 규칙이 한 곳(canRead)에만 있어야 일기는 안 보이는데 댓글은 보이는
     * 식의 어긋남이 생기지 않는다.
     *
     * readOnly는 이 메서드를 단독 호출할 때만 의미가 있고, 쓰기 트랜잭션(댓글 작성 등)에서
     * 호출하면 그 트랜잭션에 합류하며 무시된다. 그래서 반환된 Diary는 호출자와 같은 영속성
     * 컨텍스트에 있는 관리 상태 엔티티다.
     */
    @Transactional(readOnly = true)
    public Diary getReadableDiaryOrThrow(Long userId, Long diaryId) {
        Diary diary = getDiaryOrThrow(diaryId);

        if (!canRead(diary, userId)) {
            throw new DiaryAccessDeniedException(diaryId);
        }

        return diary;
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

    /**
     * 두 피드가 공유하는 변환. 목록 조회와 마찬가지로 작성자를 한 번에 가져와 메모리에서 묶는다.
     */
    private List<FeedItemResponse> toFeedItems(List<Diary> diaries) {
        Map<Long, User> authors = findAuthors(diaries.stream().map(Diary::getId).toList());

        return diaries.stream()
                .map(diary -> {
                    User author = authors.get(diary.getId());
                    return new FeedItemResponse(
                            diary.getId(),
                            diary.getTitle(),
                            diary.getContent(),
                            diary.getFinalImgUrl(),
                            diary.getCreatedAt(),
                            categoryId(diary),
                            categoryName(diary),
                            author == null ? null : new FeedUserResponse(
                                    author.getId(), author.getNickname(), author.getProfileImageUrl()),
                            diary.getId(),
                            diary.getFinalImgUrl()
                    );
                })
                .toList();
    }

    private long cursorOrFirstPage(Long cursor) {
        return cursor == null ? FIRST_PAGE_CURSOR : cursor;
    }

    /**
     * 잘못된 limit으로 에러를 주기보다 조용히 보정한다 — 피드는 클라이언트가 스크롤하며
     * 반복 호출하는 경로라, 값 하나 때문에 화면이 비는 것보다 기본값으로 굴러가는 편이 낫다.
     */
    private int pageSize(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 댓글·좋아요 알림의 수신자를 정하려고 열어둔 진입점. 작성자 판정이 목록·피드와 같은
     * findAuthors를 타므로, 화면에 작성자로 보이는 사람과 알림을 받는 사람이 항상 일치한다.
     *
     * @return 협업자가 모두 탈퇴해 작성자를 특정할 수 없으면 null
     */
    @Transactional(readOnly = true)
    public Long findAuthorId(Long diaryId) {
        User author = findAuthor(diaryId);
        return author == null ? null : author.getId();
    }

    /**
     * 랭킹 목록이 작성자를 한 번에 채우려고 쓰는 진입점. 목록·피드·알림과 같은 findAuthors를
     * 타므로 어느 화면에서든 같은 사람이 작성자로 나온다.
     */
    @Transactional(readOnly = true)
    public Map<Long, User> findAuthorsByDiaryIds(List<Long> diaryIds) {
        return findAuthors(diaryIds);
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
     * 분류가 없는 일기는 categoryId·categoryName이 함께 null이 된다. 목록 경로는
     * category를 fetch join으로 미리 가져오므로 여기서 추가 쿼리가 나가지 않는다.
     */
    private Long categoryId(Diary diary) {
        Category category = diary.getCategory();
        return category == null ? null : category.getId();
    }

    private String categoryName(Diary diary) {
        Category category = diary.getCategory();
        return category == null ? null : category.getName();
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
