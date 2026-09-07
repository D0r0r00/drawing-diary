package com.drawingdiary.backend.domain.room.exception;

public class RoomSubmitContentMissingException extends RuntimeException {

    public RoomSubmitContentMissingException(String field) {
        super("발행하려면 %s이(가) 필요합니다. 요청 본문에 담거나 임시 저장해두세요.".formatted(field));
    }
}
