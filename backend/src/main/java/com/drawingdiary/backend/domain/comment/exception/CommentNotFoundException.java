package com.drawingdiary.backend.domain.comment.exception;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long commentId) {
        super("댓글을 찾을 수 없습니다: " + commentId);
    }
}
