package com.drawingdiary.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 스타일 한 개의 결과. 성공하면 imageUrl이 차고 error가 null, 실패하면 반대다.
 * 실패한 스타일도 응답에서 빼지 않는 이유는, 프론트가 "3개 중 2개만 나왔다"는 것을
 * 알아야 재시도 버튼을 그 자리에 그릴 수 있기 때문이다.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record GuideItemResponse(
        String style,
        String styleName,
        String imageUrl,
        String error
) {
    public static GuideItemResponse success(String style, String styleName, String imageUrl) {
        return new GuideItemResponse(style, styleName, imageUrl, null);
    }

    public static GuideItemResponse failure(String style, String styleName, String error) {
        return new GuideItemResponse(style, styleName, null, error);
    }
}
