package com.drawingdiary.backend.domain.comment.dto;

import java.time.LocalDateTime;

public record CommentCreateResponse(
        Long id,
        String content,
        LocalDateTime createdAt
) {
}
