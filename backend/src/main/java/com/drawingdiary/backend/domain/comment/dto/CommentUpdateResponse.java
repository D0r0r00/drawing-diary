package com.drawingdiary.backend.domain.comment.dto;

import java.time.LocalDateTime;

public record CommentUpdateResponse(
        Long id,
        String content,
        LocalDateTime updatedAt
) {
}
