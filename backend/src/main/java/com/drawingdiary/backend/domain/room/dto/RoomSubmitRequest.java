package com.drawingdiary.backend.domain.room.dto;

import com.drawingdiary.backend.domain.diary.Visibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * title·content·canvasData는 선택이다. 생략하면 방에 임시 저장해둔 값이 쓰이고, 둘 다
 * 없으면 400으로 막힌다(RoomSubmitContentMissingException).
 *
 * tags는 태그 <b>이름</b>의 배열이다(id가 아니다). 없는 이름은 발행 시 자동으로 만들어진다.
 * 생략하거나 null이면 태그 없이 발행된다.
 *
 * 예전에는 title·content가 @NotBlank였는데, 그러면 자동 저장으로 이미 서버에 있는 내용을
 * 발행 요청에 다시 실어 보내야만 했다. 빈 문자열을 보내는 경우는 여전히 400이라
 * 기존 클라이언트가 받던 응답은 달라지지 않는다.
 */
public record RoomSubmitRequest(
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        String content,

        @Size(max = 500, message = "이미지 URL이 너무 깁니다.")
        String finalImg,

        @NotNull(message = "공개 범위를 선택해주세요.")
        Visibility visibility,

        Long categoryId,

        String canvasData,

        List<String> tags
) {
}
