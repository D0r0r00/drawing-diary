package com.drawingdiary.backend.domain.diary;

import com.drawingdiary.backend.domain.diary.dto.FeedItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 두 피드 모두 일기 목록이지만 경로가 /api/diaries 아래가 아니라서 DiaryController와 분리했다.
 * 조회 로직 자체는 DiaryService에 있어 공개 범위 판정이 한곳에 모인다.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeedController {

    private final DiaryService diaryService;

    @GetMapping("/feed")
    public ResponseEntity<List<FeedItemResponse>> feed(
            Authentication authentication,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(diaryService.findFeed(currentUserId(authentication), cursor, limit));
    }

    /**
     * 탐색은 PUBLIC만 내려가므로 누가 부르든 결과가 같다. 그래도 로그인은 필요하다
     * (SecurityConfig에서 /api/auth/** 외 전부 인증 대상).
     */
    @GetMapping("/explore")
    public ResponseEntity<List<FeedItemResponse>> explore(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(diaryService.findExplore(cursor, limit));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
