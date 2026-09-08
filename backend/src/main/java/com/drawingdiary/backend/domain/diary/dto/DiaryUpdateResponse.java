package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.tag.dto.TagResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param tags 수정 <b>후</b>의 태그 전체. 태그를 보내지 않아 그대로 둔 경우에도 현재 값이
 *             내려가므로, 프론트는 응답만 보고 화면을 다시 그리면 된다.
 */
public record DiaryUpdateResponse(
        Long id,
        String title,
        LocalDateTime updatedAt,
        List<TagResponse> tags
) {
}
