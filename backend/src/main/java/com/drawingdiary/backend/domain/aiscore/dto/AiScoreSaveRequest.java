package com.drawingdiary.backend.domain.aiscore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * AI 서버가 돌려주는 두 점수만 받는다. 좋아요 점수와 총점은 백엔드가 계산하므로
 * 요청에 실어 보내도 무시된다(필드 자체가 없다).
 */
public record AiScoreSaveRequest(
        @NotNull(message = "relevanceScore를 입력해주세요.")
        @Min(value = 0, message = "relevanceScore는 0 이상이어야 합니다.")
        @Max(value = 100, message = "relevanceScore는 100 이하여야 합니다.")
        Integer relevanceScore,

        @NotNull(message = "colorScore를 입력해주세요.")
        @Min(value = 0, message = "colorScore는 0 이상이어야 합니다.")
        @Max(value = 100, message = "colorScore는 100 이하여야 합니다.")
        Integer colorScore
) {
}
