package com.drawingdiary.backend.domain.aiscore;

import com.drawingdiary.backend.domain.aiscore.dto.AiScoreResponse;
import com.drawingdiary.backend.domain.aiscore.dto.AiScoreSaveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries/{diaryId}/scores")
@RequiredArgsConstructor
public class AiScoreController {

    private final AiScoreService aiScoreService;

    /**
     * 재호출하면 갱신이라 201이 아니라 200이다 — 같은 일기에 두 번 부르면 새 자원이 생기는
     * 게 아니라 기존 점수가 덮어써진다.
     */
    @PostMapping
    public ResponseEntity<AiScoreResponse> save(
            Authentication authentication,
            @PathVariable Long diaryId,
            @Valid @RequestBody AiScoreSaveRequest request
    ) {
        return ResponseEntity.ok(aiScoreService.save(currentUserId(authentication), diaryId, request));
    }

    @GetMapping
    public ResponseEntity<AiScoreResponse> find(Authentication authentication, @PathVariable Long diaryId) {
        return ResponseEntity.ok(aiScoreService.find(currentUserId(authentication), diaryId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
