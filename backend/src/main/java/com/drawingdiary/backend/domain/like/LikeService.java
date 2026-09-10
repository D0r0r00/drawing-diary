package com.drawingdiary.backend.domain.like;

import com.drawingdiary.backend.domain.aiscore.AiScoreService;
import com.drawingdiary.backend.domain.diary.DiaryService;
import com.drawingdiary.backend.domain.like.dto.LikeResponse;
import com.drawingdiary.backend.domain.like.dto.LikeUserResponse;
import com.drawingdiary.backend.domain.notification.NotificationService;
import com.drawingdiary.backend.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final DiaryService diaryService;
    private final NotificationService notificationService;
    private final AiScoreService aiScoreService;

    /**
     * 멱등: 이미 눌러둔 좋아요를 다시 눌러도 에러 없이 현재 상태를 그대로 돌려준다.
     * 일기를 볼 수 없는 사람은 좋아요도 누를 수 없다.
     */
    @Transactional
    public LikeResponse like(Long userId, Long diaryId) {
        diaryService.getReadableDiaryOrThrow(userId, diaryId);

        // insertIfAbsent가 실제로 넣은 행 수. 이미 눌러둔 상태에서 다시 호출하면 0이라
        // 알림이 다시 가지 않는다 — 멱등한 API가 알림만 계속 쌓는 일을 막는다.
        boolean newlyLiked = likeRepository.insertIfAbsent(diaryId, userId) > 0;

        if (newlyLiked) {
            notificationService.notify(
                    diaryService.findAuthorId(diaryId), userId, NotificationType.LIKE, diaryId);
            // 좋아요 수가 랭킹 점수에 들어가므로 실제로 변했을 때만 총점을 다시 계산한다.
            // 알림과 같은 자리인 이유도 같다 — 멱등 재호출로는 아무것도 변하지 않았다.
            aiScoreService.refreshLikeScore(diaryId);
        }

        return new LikeResponse(diaryId, true, likeRepository.countByDiaryId(diaryId));
    }

    /**
     * 좋아요를 누른 사람 목록. 일기를 볼 수 없는 사람은 누가 눌렀는지도 볼 수 없다 —
     * 댓글 목록과 같이 권한 판정을 DiaryService에 맡겨 규칙이 한 곳에만 있게 한다.
     *
     * <p>탈퇴한 계정은 리포지토리 단계에서 빠지므로, 이 목록의 길이가 좋아요 응답의
     * likeCount보다 작을 수 있다(findUsersByDiaryId 주석 참고).
     */
    @Transactional(readOnly = true)
    public List<LikeUserResponse> findLikedUsers(Long userId, Long diaryId) {
        diaryService.getReadableDiaryOrThrow(userId, diaryId);

        return likeRepository.findUsersByDiaryId(diaryId).stream()
                .map(user -> new LikeUserResponse(
                        user.getId(), user.getNickname(), user.getProfileImageUrl()))
                .toList();
    }

    /**
     * 멱등: 누른 적 없는 좋아요를 취소해도 호출자가 원한 상태(취소됨)이므로 200이다.
     * 등록과 달리 조회 권한을 보지 않는데, 취소는 이미 남긴 흔적을 지우는 일이라
     * 나중에 일기가 비공개로 바뀌어도 되돌릴 수 있어야 하기 때문이다.
     */
    @Transactional
    public LikeResponse unlike(Long userId, Long diaryId) {
        boolean actuallyRemoved = likeRepository.deleteByDiaryIdAndUserId(diaryId, userId) > 0;

        if (actuallyRemoved) {
            aiScoreService.refreshLikeScore(diaryId);
        }

        return new LikeResponse(diaryId, false, likeRepository.countByDiaryId(diaryId));
    }
}
