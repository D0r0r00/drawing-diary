package com.drawingdiary.backend.domain.room.exception;

public class OwnerCannotLeaveRoomException extends RuntimeException {

    public OwnerCannotLeaveRoomException(Long roomId) {
        super("방장은 방을 나갈 수 없습니다. 방을 삭제해주세요: " + roomId);
    }
}
