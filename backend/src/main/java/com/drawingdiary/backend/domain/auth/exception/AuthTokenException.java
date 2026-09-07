package com.drawingdiary.backend.domain.auth.exception;

import com.drawingdiary.backend.security.AuthErrorCode;
import lombok.Getter;

/**
 * 토큰 재발급처럼 컨트롤러까지 도달한 경로에서 쓰는 인증 실패 예외.
 * 필터 단계의 실패는 EntryPoint가 직접 응답하므로 이 예외를 거치지 않는다.
 */
@Getter
public class AuthTokenException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthTokenException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
