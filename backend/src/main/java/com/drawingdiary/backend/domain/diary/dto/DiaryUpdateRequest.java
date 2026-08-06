package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.diary.Visibility;
import jakarta.validation.constraints.Size;

/**
 * 부분 수정: 세 필드 모두 선택이고, null인 필드는 기존 값을 유지한다.
 * 값을 보낼 때만 형식을 검증하므로 @NotBlank는 쓰지 않는다.
 */
public record DiaryUpdateRequest(
        @Size(min = 1, max = 100, message = "제목은 1자 이상 100자 이하여야 합니다.")
        String title,

        @Size(min = 1, message = "내용을 입력해주세요.")
        String textContent,

        Visibility visibility
) {
}
