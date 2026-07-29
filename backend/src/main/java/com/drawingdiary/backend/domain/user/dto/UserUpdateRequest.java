package com.drawingdiary.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        @Size(max = 500, message = "프로필 이미지 URL이 너무 깁니다.")
        String profileImageUrl
) {
}
