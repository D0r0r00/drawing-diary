package com.drawingdiary.backend.domain.room.dto;

import jakarta.validation.constraints.Size;

/**
 * 세 필드 모두 선택. 보낸 것만 저장되고 보내지 않은 것은 기존 값이 유지된다.
 * 자동 저장이 캔버스만 반복해서 보내는 상황을 그대로 지원하기 위한 형태다.
 */
public record RoomCanvasSaveRequest(
        String canvasData,

        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        String content
) {
}
