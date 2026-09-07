package com.drawingdiary.backend.domain.category;

import com.drawingdiary.backend.domain.category.dto.CategoryCreateRequest;
import com.drawingdiary.backend.domain.category.dto.CategoryDeleteResponse;
import com.drawingdiary.backend.domain.category.dto.CategoryResponse;
import com.drawingdiary.backend.domain.category.dto.CategoryUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카테고리는 사용자별로만 존재해서 경로가 /api/users/me 아래에 놓인다.
 * UserController와 분리한 이유는 Room/Diary처럼 도메인 단위로 나누는 기존 구조를 따르기 위함이다.
 */
@RestController
@RequestMapping("/api/users/me/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findMine(Authentication authentication) {
        return ResponseEntity.ok(categoryService.findMine(currentUserId(authentication)));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            Authentication authentication,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(currentUserId(authentication), request));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> update(
            Authentication authentication,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(categoryService.update(currentUserId(authentication), categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<CategoryDeleteResponse> delete(
            Authentication authentication,
            @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(categoryService.delete(currentUserId(authentication), categoryId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
