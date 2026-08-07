package com.drawingdiary.backend.domain.like;

import com.drawingdiary.backend.domain.like.dto.LikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries/{diaryId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

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
