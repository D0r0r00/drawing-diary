package com.drawingdiary.backend.domain.diary;

import com.drawingdiary.backend.domain.diary.dto.DiaryDeleteResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryDetailResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryListResponse;
import com.drawingdiary.backend.domain.diary.dto.DiaryUpdateRequest;
import com.drawingdiary.backend.domain.diary.dto.DiaryUpdateResponse;
import com.drawingdiary.backend.domain.diary.dto.MyDiaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping
    public ResponseEntity<List<DiaryListResponse>> findAll() {
        return ResponseEntity.ok(diaryService.findAll());
    }

    @GetMapping("/my")
    public ResponseEntity<List<MyDiaryResponse>> findMine(Authentication authentication) {
        return ResponseEntity.ok(diaryService.findMine(currentUserId(authentication)));
    }

    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryDetailResponse> find(Authentication authentication, @PathVariable Long diaryId) {
        return ResponseEntity.ok(diaryService.find(currentUserId(authentication), diaryId));
    }

    @PatchMapping("/{diaryId}")
    public ResponseEntity<DiaryUpdateResponse> update(
            Authentication authentication,
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request
    ) {
        return ResponseEntity.ok(diaryService.update(currentUserId(authentication), diaryId, request));
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<DiaryDeleteResponse> delete(Authentication authentication, @PathVariable Long diaryId) {
        return ResponseEntity.ok(diaryService.delete(currentUserId(authentication), diaryId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
