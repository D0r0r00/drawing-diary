package com.drawingdiary.backend.domain.comment.exception;

/**
 * 수정(PATCH)과 삭제(DELETE)가 함께 쓴다. 두 경로의 판정이 "작성자 본인인가" 하나로
 * 같아서 예외도 하나로 두고, 메시지는 어느 쪽에서 나와도 말이 되게 적는다.
 */
public class NotCommentAuthorException extends RuntimeException {

    public NotCommentAuthorException(Long commentId) {
        super("본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다: " + commentId);
    }
}
