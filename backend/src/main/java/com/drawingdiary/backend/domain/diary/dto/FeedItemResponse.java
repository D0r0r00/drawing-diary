package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.tag.dto.TagResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * /api/feed·/api/explore·/api/explore/random이 공유하는 카드 형태.
 *
 * @param tags    카드에 표시할 태그. 태그가 없으면 null이 아니라 <b>빈 배열</b>이다.
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
        List<TagResponse> tags,
        @Deprecated Long diaryId,
        @Deprecated String img
) {
}
