package com.drawingdiary.backend.domain.image.exception;

public class UnsupportedImageTypeException extends RuntimeException {

    public UnsupportedImageTypeException(String contentType) {
        super("지원하지 않는 이미지 형식입니다: " + contentType);
    }
}
