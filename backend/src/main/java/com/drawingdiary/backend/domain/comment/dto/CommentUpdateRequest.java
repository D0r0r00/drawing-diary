package com.drawingdiary.backend.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 제한은 작성(CommentCreateRequest)과 같다. 작성은 통과하는데 수정은 막히거나 그 반대가
 * 되면 사용자가 이유를 알 수 없으므로 두 곳을 같은 값으로 둔다.
 */
public record CommentUpdateRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
        String content
) {
}
