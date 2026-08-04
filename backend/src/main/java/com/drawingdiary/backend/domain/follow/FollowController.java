package com.drawingdiary.backend.domain.follow;

import com.drawingdiary.backend.domain.follow.dto.FollowUserResponse;
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
@RequestMapping("/api/users/{userId}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/follow")
    public ResponseEntity<Void> follow(Authentication authentication, @PathVariable Long userId) {
        followService.follow(currentUserId(authentication), userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/follow")
    public ResponseEntity<Void> unfollow(Authentication authentication, @PathVariable Long userId) {
        followService.unfollow(currentUserId(authentication), userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/followers")
    public ResponseEntity<List<FollowUserResponse>> followers(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.findFollowers(userId));
    }

    @GetMapping("/followings")
    public ResponseEntity<List<FollowUserResponse>> followings(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.findFollowings(userId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
