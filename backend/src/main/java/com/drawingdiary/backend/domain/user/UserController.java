package com.drawingdiary.backend.domain.user;

import com.drawingdiary.backend.domain.user.dto.UserResponse;
import com.drawingdiary.backend.domain.user.dto.UserSearchResponse;
import com.drawingdiary.backend.domain.user.dto.UserUpdateRequest;
import com.drawingdiary.backend.domain.user.dto.UserUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(userService.getMe(currentUserId(authentication)));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserUpdateResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateMe(currentUserId(authentication), request));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.search(keyword));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
