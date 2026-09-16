package com.drawingdiary.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 형식 검사(@ValidLoginId)를 붙이지 않는다. 틀린 형식이든 없는 아이디든 똑같이 401이어야
 * "이 아이디는 형식상 존재할 수 없다"는 정보조차 새지 않는다.
 */
public record LoginRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
