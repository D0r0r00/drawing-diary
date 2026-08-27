package com.drawingdiary.backend.domain.diary.dto;

/**
 * /api/feed와 /api/explore가 공유하는 카드 형태. 다음 페이지 커서는 마지막 항목의 diaryId다.
 */
public record FeedItemResponse(
        Long diaryId,
        String img,
        String title,
        FeedUserResponse user
) {
}
