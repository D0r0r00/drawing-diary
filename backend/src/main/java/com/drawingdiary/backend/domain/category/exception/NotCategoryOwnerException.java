package com.drawingdiary.backend.domain.category.exception;

public class NotCategoryOwnerException extends RuntimeException {

    public NotCategoryOwnerException(Long categoryId) {
        super("본인의 카테고리가 아닙니다: " + categoryId);
    }
}
