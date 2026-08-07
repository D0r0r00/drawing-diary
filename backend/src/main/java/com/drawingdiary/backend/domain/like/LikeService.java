package com.drawingdiary.backend.domain.like;

import com.drawingdiary.backend.domain.diary.DiaryService;
import com.drawingdiary.backend.domain.like.dto.LikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final DiaryService diaryService;

    /**
     * 멱등: 이미 눌러둔 좋아요를 다시 눌러도 에러 없이 현재 상태를 그대로 돌려준다.
     * 일기를 볼 수 없는 사람은 좋아요도 누를 수 없다.
     */
    @Transactional
    public LikeResponse like(Long userId, Long diaryId) {
        diaryService.getReadableDiaryOrThrow(userId, diaryId);

        likeRepository.insertIfAbsent(diaryId, userId);

        return new LikeResponse(diaryId, true, likeRepository.countByDiaryId(diaryId));
    }

    /**
     * 멱등: 누른 적 없는 좋아요를 취소해도 호출자가 원한 상태(취소됨)이므로 200이다.
     * 등록과 달리 조회 권한을 보지 않는데, 취소는 이미 남긴 흔적을 지우는 일이라
     * 나중에 일기가 비공개로 바뀌어도 되돌릴 수 있어야 하기 때문이다.
     */
    @Transactional
    public LikeResponse unlike(Long userId, Long diaryId) {
        likeRepository.deleteByDiaryIdAndUserId(diaryId, userId);

        return new LikeResponse(diaryId, false, likeRepository.countByDiaryId(diaryId));
    }
}
