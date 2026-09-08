package com.drawingdiary.backend.domain.tag.exception;

public class InvalidTagNameException extends RuntimeException {

    public InvalidTagNameException(int maxLength) {
        super("태그는 " + maxLength + "자 이하여야 합니다.");
    }
}
