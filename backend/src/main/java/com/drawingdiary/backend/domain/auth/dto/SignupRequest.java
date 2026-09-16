package com.drawingdiary.backend.domain.auth.dto;

import com.drawingdiary.backend.domain.auth.validation.ValidLoginId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param loginId 대소문자 무관. 저장은 소문자로 된다(LoginIdPolicy)
 * @param email   선택. 로그인에는 쓰지 않고 비밀번호 찾기 같은 연락 수단으로만 보관한다.
 *                빈 문자열은 "안 보냄"과 같게 취급하고, 값이 있으면 형식만 검사한다
 *                (@Email은 null과 빈 문자열을 통과시킨다). 중복은 허용한다.
 */
public record SignupRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @ValidLoginId
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email
) {
}
