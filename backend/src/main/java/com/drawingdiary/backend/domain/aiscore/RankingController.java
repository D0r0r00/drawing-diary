package com.drawingdiary.backend.domain.aiscore;

import com.drawingdiary.backend.domain.aiscore.dto.MyRankingItemResponse;
import com.drawingdiary.backend.domain.aiscore.dto.RankingItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 랭킹은 일기 하나에 매달린 자원이 아니라 전체를 가로지르는 목록이라 /api/diaries 아래가
 * 아닌 최상위 경로에 둔다(피드가 FeedController로 갈라진 것과 같은 이유).
 */
@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final AiScoreService aiScoreService;

    /**
     * 피드·탐색과 달리 cursor가 아니라 offset이다. 정렬 키가 (total_score, diaryId) 두 개라
     * diaryId 커서로는 자를 수 없고, 화면에 찍을 rank가 시작 위치를 알아야 나오기 때문이다.
     */
    @GetMapping
    public ResponseEntity<List<RankingItemResponse>> ranking(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(aiScoreService.findRanking(offset, limit));
    }

    @GetMapping("/me")
    public ResponseEntity<List<MyRankingItemResponse>> myRanking(Authentication authentication) {
        return ResponseEntity.ok(aiScoreService.findMyRanking((Long) authentication.getPrincipal()));
    }
}
