package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.diary.Visibility;
import com.drawingdiary.backend.domain.tag.dto.TagResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param tags 피드·탐색·상세와 같은 규칙: 태그가 없으면 null이 아니라 <b>빈 배열</b>이다.
 */
public record MyDiaryResponse(
        Long id,
        String title,
        String content,
        String thumbnailUrl,
        LocalDateTime createdAt,
        Long categoryId,
        String categoryName,
        Visibility visibility,
        List<TagResponse> tags
) {
}
