package com.drawingdiary.backend.domain.user.dto;

import java.time.LocalDate;

/**
 * 활동 잔디 한 칸. 일기가 없는 날은 <b>배열에 아예 없다</b> — 한 달치 빈 칸까지 내려주면
 * 응답의 대부분이 count=0이 되고, 어차피 달력 격자는 프론트가 그린다.
 *
 * @param date  yyyy-MM-dd. LocalDate라 Jackson이 시간대 없이 직렬화한다.
 * @param count 그날 쓴 일기 수 중 <b>요청자가 볼 수 있는 것만</b>
 */
public record ActivityItemResponse(
        LocalDate date,
        long count
) {
}
