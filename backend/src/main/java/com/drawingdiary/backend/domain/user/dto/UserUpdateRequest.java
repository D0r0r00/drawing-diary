package com.drawingdiary.backend.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 세 필드 모두 선택적이다. null(= 필드를 보내지 않음)은 "그대로 두라"는 뜻이고,
 * 빈 문자열은 "값을 지우라"는 뜻이다. 단 nickname은 필수 컬럼이라 비울 수 없어서,
 * 보냈다면 공백이 아닌 문자가 하나는 있어야 한다.
 *
 * @Pattern은 null을 검사하지 않고 통과시키므로(Bean Validation 명세) "생략 가능하지만
 * 보냈다면 비어 있으면 안 된다"는 규칙을 그대로 표현할 수 있다. @NotBlank로는
 * 생략 자체가 막혀버린다.
 */
public record UserUpdateRequest(
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "닉네임을 입력해주세요.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        @Size(max = 500, message = "프로필 이미지 URL이 너무 깁니다.")
        String profileImageUrl,

        @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
        String bio
) {
}
