package com.drawingdiary.backend.domain.diary.dto;

import com.drawingdiary.backend.domain.diary.Visibility;

import java.time.LocalDateTime;

/**
 * thumbnailUrl과 imageUrl은 둘 다 diaries.final_img_url이라 항상 같은 값이다.
 * 목록 카드(thumbnailUrl)와 상세 화면(imageUrl)이 같은 컴포넌트를 공유할 수 있도록
 * 프론트 요청에 맞춰 두 이름으로 함께 내려준다. 원본과 축소본을 따로 저장하게 되면
 * 그때 thumbnailUrl만 갈라지면 된다.
 *
 * @param textContent content와 같은 값. 기존 프론트가 쓰던 이름이라 남겨둔 것으로,
 *                    새로 붙이는 화면은 content를 쓸 것.
 */
public record DiaryDetailResponse(
        Long id,
        String title,
        String content,
        String thumbnailUrl,
        String imageUrl,
        LocalDateTime createdAt,
        Long categoryId,
        String categoryName,
        Visibility visibility,
        String canvasData,
        @Deprecated String textContent
) {
}
