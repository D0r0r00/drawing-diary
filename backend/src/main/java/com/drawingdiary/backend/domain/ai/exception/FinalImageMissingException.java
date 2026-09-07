package com.drawingdiary.backend.domain.ai.exception;

public class FinalImageMissingException extends RuntimeException {

    public FinalImageMissingException(Long diaryId) {
        super("평가할 완성 이미지가 없습니다: " + diaryId);
    }
}
