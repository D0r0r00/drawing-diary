package com.drawingdiary.backend.domain.diary.dto;

import java.time.LocalDateTime;

/**
 * /api/feed와 /api/explore가 공유하는 카드 형태.
 *
 * @param diaryId id와 같은 값. 다음 페이지 커서로 쓰던 필드라 기존 프론트를 위해 남겨둔다.
 * @param img     thumbnailUrl과 같은 값. 위와 같은 이유로 남겨둔 옛 이름.
 */
public record FeedItemResponse(
        Long id,
        String title,
        String content,
        String thumbnailUrl,
        LocalDateTime createdAt,
        Long categoryId,
        String categoryName,
        FeedUserResponse user,
        @Deprecated Long diaryId,
        @Deprecated String img
) {
}
