package com.drawingdiary.backend.domain.like;

import com.drawingdiary.backend.domain.like.dto.LikeResponse;
import com.drawingdiary.backend.domain.like.dto.LikeUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/diaries/{diaryId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 좋아요 등록(POST)·취소(DELETE)와 같은 경로에 메서드만 다르게 붙인다.
     * 같은 자원(그 일기의 좋아요)을 목록으로 읽는 것이라 새 경로를 파지 않는다.
     */
    @GetMapping
    public ResponseEntity<List<LikeUserResponse>> findLikedUsers(
            Authentication authentication,
            @PathVariable Long diaryId
    ) {
        return ResponseEntity.ok(likeService.findLikedUsers(currentUserId(authentication), diaryId));
    }

    @PostMapping
    public ResponseEntity<LikeResponse> like(Authentication authentication, @PathVariable Long diaryId) {
        return ResponseEntity.ok(likeService.like(currentUserId(authentication), diaryId));
    }

    @DeleteMapping
    public ResponseEntity<LikeResponse> unlike(Authentication authentication, @PathVariable Long diaryId) {
        return ResponseEntity.ok(likeService.unlike(currentUserId(authentication), diaryId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
