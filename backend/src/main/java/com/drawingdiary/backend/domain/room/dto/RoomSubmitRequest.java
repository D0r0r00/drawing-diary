package com.drawingdiary.backend.domain.room.dto;

import com.drawingdiary.backend.domain.diary.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomSubmitRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        @Size(max = 500, message = "이미지 URL이 너무 깁니다.")
        String finalImg,

        @NotNull(message = "공개 범위를 선택해주세요.")
        Visibility visibility,

        Long categoryId
) {
}
