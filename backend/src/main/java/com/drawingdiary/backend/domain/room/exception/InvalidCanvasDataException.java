package com.drawingdiary.backend.domain.room.exception;

public class InvalidCanvasDataException extends RuntimeException {

    public InvalidCanvasDataException() {
        super("canvasData는 Base64 문자열이어야 합니다.");
    }
}
