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

    /**
     * 친구 랭킹 — 내가 팔로우하는 사람들이 참여한 일기.
     *
     * <h4>rank의 의미: 이 목록 안에서의 순위다</h4>
     * 같은 이름의 필드지만 /api/rankings/me와 뜻이 다르니 주의.
     * <ul>
     *   <li>여기와 GET /api/rankings — <b>그 목록에서 몇 번째</b>(rank = offset + 순번)</li>
     *   <li>GET /api/rankings/me — <b>전체 랭킹에서 몇 위</b></li>
     * </ul>
     * 친구 랭킹을 전체 순위로 매기면 화면에 37위·102위·415위처럼 찍혀 리더보드로 읽히지
     * 않고, offset을 넘길 때 rank가 건너뛰어 "몇 번째 항목인지"도 알 수 없게 된다.
     * 내 순위는 "전체에서 어디쯤인가"가 곧 질문이라 반대로 전역 순위여야 한다.
     */
    @GetMapping("/friends")
    public ResponseEntity<List<RankingItemResponse>> friendRanking(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(
                aiScoreService.findFriendRanking((Long) authentication.getPrincipal(), offset, limit));
    }

    /**
     * rank가 <b>전체 랭킹 기준</b>이다(위 friendRanking 주석 참고).
     */
    @GetMapping("/me")
    public ResponseEntity<List<MyRankingItemResponse>> myRanking(Authentication authentication) {
        return ResponseEntity.ok(aiScoreService.findMyRanking((Long) authentication.getPrincipal()));
    }
}
