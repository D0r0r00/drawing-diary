package com.drawingdiary.backend.domain.ai;

import com.drawingdiary.backend.domain.aiscore.dto.AiScoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 수동 저장(POST /api/diaries/{id}/scores)과 응답 형식이 같다. 다른 점은 점수를 요청 바디로
 * 받는 대신 백엔드가 AI 서버에 물어본다는 것뿐이라, 프론트는 두 경로를 같은 코드로 처리하면 된다.
 */
@RestController
@RequestMapping("/api/diaries/{diaryId}/ai-score")
@RequiredArgsConstructor
public class AiDiaryScoreController {

    private final AiDiaryScoreService aiDiaryScoreService;

    @PostMapping
    public ResponseEntity<AiScoreResponse> score(Authentication authentication, @PathVariable Long diaryId) {
        return ResponseEntity.ok(
                aiDiaryScoreService.score((Long) authentication.getPrincipal(), diaryId));
    }
}
