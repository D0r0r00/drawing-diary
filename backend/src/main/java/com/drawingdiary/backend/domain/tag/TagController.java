package com.drawingdiary.backend.domain.tag;

import com.drawingdiary.backend.domain.tag.dto.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 태그는 전역 사전이라 사용자별 경로가 아니다. 검색만 열려 있고 태그를 만드는 경로는 따로
 * 없다 — 일기에 태그를 달 때(발행·수정) 없는 이름이면 그때 자동으로 생긴다.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * q가 없어도 400이 아니라 빈 배열이다. 입력창이 비어 있는 상태에서도 자동완성이 그대로
     * 호출되는 경로라, 값이 없는 것은 오류가 아니라 "아직 검색어가 없음"이다.
     */
    @GetMapping("/search")
    public ResponseEntity<List<TagResponse>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(tagService.search(q));
    }
}
