package com.drawingdiary.backend.domain.room.exception;

public class NotRoomOwnerException extends RuntimeException {

    public NotRoomOwnerException(Long roomId) {
        super("방장만 수행할 수 있습니다: " + roomId);
    }
}
