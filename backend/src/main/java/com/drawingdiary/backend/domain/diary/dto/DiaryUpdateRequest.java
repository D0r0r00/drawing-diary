package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.diary.Visibility;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 부분 수정: 네 필드 모두 선택이고, null인 필드는 기존 값을 유지한다.
 * 값을 보낼 때만 형식을 검증하므로 @NotBlank는 쓰지 않는다.
 *
 * @param tags 넘긴 목록으로 <b>통째로 교체</b>한다(추가가 아니다). 생략하거나 null이면 태그를
 *             건드리지 않고, 빈 배열 []을 보내면 전부 떼어낸다. 다른 필드와 규칙이 같도록
 *             "null = 안 바꿈"을 유지한 것이라, 태그를 지우려면 []을 명시해야 한다.
 */
public record DiaryUpdateRequest(
        @Size(min = 1, max = 100, message = "제목은 1자 이상 100자 이하여야 합니다.")
        String title,

        @Size(min = 1, message = "내용을 입력해주세요.")
        String textContent,

        Visibility visibility,

        List<String> tags
) {
}
