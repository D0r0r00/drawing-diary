package com.drawingdiary.backend.domain.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이름 외에 바꿀 것이 없어 부분 수정을 지원하지 않는다. 프로필 수정과 달리 빈 요청이
 * 의미를 갖지 않으므로 name을 필수로 둔다.
 */
public record CategoryUpdateRequest(
        @NotBlank(message = "카테고리 이름을 입력해주세요.")
        @Size(max = 50, message = "카테고리 이름은 50자 이하여야 합니다.")
        String name
) {
}
