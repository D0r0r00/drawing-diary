package com.drawingdiary.backend.domain.follow.exception;

public class AlreadyFollowingException extends RuntimeException {

    public AlreadyFollowingException(Long userId) {
        super("이미 팔로우 중인 사용자입니다: " + userId);
    }
}
