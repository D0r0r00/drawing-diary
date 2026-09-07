package com.drawingdiary.backend.domain.category.exception;

public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String name) {
        super("이미 존재하는 카테고리입니다: " + name);
    }
}
