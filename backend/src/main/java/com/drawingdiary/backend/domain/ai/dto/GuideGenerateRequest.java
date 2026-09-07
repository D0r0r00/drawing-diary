package com.drawingdiary.backend.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuideGenerateRequest(
        @NotBlank(message = "일기 내용을 입력해주세요.")
        @Size(max = 5000, message = "일기 내용은 5000자 이하여야 합니다.")
        String text
) {
}
