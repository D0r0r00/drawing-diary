package com.drawingdiary.backend.domain.user;

import com.drawingdiary.backend.domain.user.dto.OtherUserResponse;
import com.drawingdiary.backend.domain.user.dto.UserResponse;
import com.drawingdiary.backend.domain.user.dto.UserSearchResponse;
import com.drawingdiary.backend.domain.user.dto.UserUpdateRequest;
import com.drawingdiary.backend.domain.user.dto.UserUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        userService.deleteMe(currentUserId(authentication));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.search(keyword));
    }

    /**
     * 남의 프로필.
     *
     * <h4>{userId:\\d+}의 정규식이 하는 일</h4>
     * 이 경로는 같은 자리를 쓰는 /api/users/me, /api/users/search와 모양이 겹친다.
     * 스프링은 리터럴 세그먼트를 경로 변수보다 먼저 고르므로 제약이 없어도 대개는 맞게
     * 매칭되지만, 그 우선순위에 기대면 "me"가 userId로 들어오는 순간 500이 난다
     * (경로 변수 타입이 Long이라 변환에서 터진다). 숫자만 받도록 못 박아두면 매칭 규칙이
     * 무엇이든 문자 경로가 이 핸들러로 샐 수 없다.
     */
    @GetMapping("/{userId:\\d+}")
    public ResponseEntity<OtherUserResponse> findOther(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(userService.findOther(currentUserId(authentication), userId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
