package com.drawingdiary.backend.domain.comment;

import com.drawingdiary.backend.domain.comment.dto.CommentCreateRequest;
import com.drawingdiary.backend.domain.comment.dto.CommentCreateResponse;
import com.drawingdiary.backend.domain.comment.dto.CommentDeleteResponse;
import com.drawingdiary.backend.domain.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 목록·작성은 일기 하위 경로지만 삭제는 commentId만으로 부르는 /api/comments라,
 * 공통 prefix를 /api로 두고 메서드마다 전체 경로를 적는다.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/diaries/{diaryId}/comments")
    public ResponseEntity<List<CommentResponse>> findAll(Authentication authentication, @PathVariable Long diaryId) {
        return ResponseEntity.ok(commentService.findAll(currentUserId(authentication), diaryId));
    }

    @PostMapping("/diaries/{diaryId}/comments")
    public ResponseEntity<CommentCreateResponse> create(
            Authentication authentication,
            @PathVariable Long diaryId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentCreateResponse response = commentService.create(currentUserId(authentication), diaryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<CommentDeleteResponse> delete(Authentication authentication, @PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.delete(currentUserId(authentication), commentId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
