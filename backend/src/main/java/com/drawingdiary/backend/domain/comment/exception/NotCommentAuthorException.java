package com.drawingdiary.backend.domain.comment.exception;

public class NotCommentAuthorException extends RuntimeException {

    public NotCommentAuthorException(Long commentId) {
        super("본인이 작성한 댓글만 삭제할 수 있습니다: " + commentId);
    }
}
