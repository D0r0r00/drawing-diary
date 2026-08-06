package com.drawingdiary.backend.domain.room.exception;

public class NotRoomMemberException extends RuntimeException {

    public NotRoomMemberException(Long roomId) {
        super("방 멤버가 아닙니다: " + roomId);
    }
}
