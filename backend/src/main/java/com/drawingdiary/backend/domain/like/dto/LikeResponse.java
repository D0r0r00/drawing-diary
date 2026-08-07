package com.drawingdiary.backend.domain.like.dto;

public record LikeResponse(
        Long diaryId,
        boolean liked,
        long likeCount
) {
}
