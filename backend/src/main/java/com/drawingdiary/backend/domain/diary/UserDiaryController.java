package com.drawingdiary.backend.domain.diary;

import com.drawingdiary.backend.domain.diary.dto.FeedItemResponse;
import com.drawingdiary.backend.domain.user.dto.ActivityItemResponse;
import com.drawingdiary.backend.domain.user.exception.InvalidActivityPeriodException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * 타인 프로필 화면이 쓰는 일기 관련 경로. 경로는 /api/users 아래지만 다루는 것이 일기라
 * UserController가 아니라 여기 둔다 — 공개 범위 판정이 DiaryService 한 곳에 모여야
 * 상세 조회와 어긋나지 않기 때문이다. FollowController가 같은 이유로 갈라져 있다.
 *
 * <h4>{userId:\d+}</h4>
 * /api/users/me/categories 같은 문자 경로가 이 컨트롤러로 새지 않도록 숫자만 받는다.
 * 지금은 겹치는 경로가 없지만, 나중에 /api/users/me/diaries가 생겨도 조용히
 * userId="me"로 매칭되는 일이 없도록 미리 못 박아둔다.
 */
@RestController
@RequestMapping("/api/users/{userId:\\d+}")
@RequiredArgsConstructor
public class UserDiaryController {

    private final DiaryService diaryService;

    /**
     * 활동 잔디가 받는 연도의 범위. 아래위로 넉넉히 열어두되, 달력에 그릴 수 없는 값
     * (연도 0, 999999 같은 것)이 그대로 쿼리로 내려가지 않게 막는 선이다.
     */
    private static final int MIN_YEAR = 2000;

    private static final int MAX_YEAR = 2100;

    @GetMapping("/diaries")
    public ResponseEntity<List<FeedItemResponse>> diaries(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(
                diaryService.findByUser(currentUserId(authentication), userId, cursor, limit));
    }

    /**
     * year·month는 필수다. 빠뜨리면 스프링이 MissingServletRequestParameterException을
     * 던지고 GlobalExceptionHandler가 400으로 바꾼다.
     *
     * <p>limit처럼 조용히 보정하지 않는 이유: 잘못된 달을 받아 이번 달로 바꿔치면 프론트가
     * 요청한 것과 다른 달의 잔디를 그리고, 화면에는 아무 이상이 없어 보인다.
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityItemResponse>> activity(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                diaryService.findActivity(currentUserId(authentication), userId, yearMonth(year, month)));
    }

    private YearMonth yearMonth(int year, int month) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new InvalidActivityPeriodException(
                    "year는 " + MIN_YEAR + "~" + MAX_YEAR + " 사이여야 합니다: " + year);
        }
        if (month < 1 || month > 12) {
            throw new InvalidActivityPeriodException("month는 1~12 사이여야 합니다: " + month);
        }
        return YearMonth.of(year, month);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
