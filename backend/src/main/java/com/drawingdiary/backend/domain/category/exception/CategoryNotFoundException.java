package com.drawingdiary.backend.domain.category.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long categoryId) {
        super("카테고리를 찾을 수 없습니다: " + categoryId);
    }
}
