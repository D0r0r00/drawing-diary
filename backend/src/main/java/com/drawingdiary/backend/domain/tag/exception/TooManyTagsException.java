package com.drawingdiary.backend.domain.tag.exception;

public class TooManyTagsException extends RuntimeException {

    public TooManyTagsException(int maxCount) {
        super("태그는 일기당 " + maxCount + "개까지 붙일 수 있습니다.");
    }
}
