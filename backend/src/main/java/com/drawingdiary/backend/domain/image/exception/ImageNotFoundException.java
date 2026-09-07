package com.drawingdiary.backend.domain.image.exception;

public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException(Long imageId) {
        super("이미지를 찾을 수 없습니다: " + imageId);
    }
}
