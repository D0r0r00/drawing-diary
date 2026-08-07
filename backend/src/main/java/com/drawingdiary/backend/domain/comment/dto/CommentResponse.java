package com.drawingdiary.backend.domain.comment.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long userId,
        String nickname,
        String content,
        LocalDateTime createdAt
) {
}
