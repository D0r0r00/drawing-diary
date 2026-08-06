package com.drawingdiary.backend.domain.room.exception;

public class RoomInviteNotFoundException extends RuntimeException {

    public RoomInviteNotFoundException(Long roomId) {
        super("초대받지 않은 방입니다: " + roomId);
    }
}
