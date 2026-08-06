package com.drawingdiary.backend.domain.room.exception;

public class RoomAlreadyFinishedException extends RuntimeException {

    public RoomAlreadyFinishedException(Long roomId) {
        super("이미 발행이 완료된 방입니다: " + roomId);
    }
}
