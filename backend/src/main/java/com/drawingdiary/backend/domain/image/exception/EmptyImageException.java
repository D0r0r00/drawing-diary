package com.drawingdiary.backend.domain.image.exception;

public class EmptyImageException extends RuntimeException {

    public EmptyImageException() {
        super("이미지 파일이 비어 있습니다.");
    }
}
