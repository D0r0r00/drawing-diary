package com.drawingdiary.backend.common.exception;

import com.drawingdiary.backend.security.AuthErrorCode;

/**
 * 인증 실패 전용 응답. 일반 에러의 {@link ErrorResponse}와 달리 code를 함께 내려서
 * 프론트가 "만료라서 refresh하면 되는 상황"과 "재로그인해야 하는 상황"을 구분할 수 있게 한다.
 *
 * 시큐리티 필터에서 끊긴 경우(EntryPoint)와 컨트롤러까지 온 경우(/api/auth/refresh)가
 * 같은 형태를 쓰도록 양쪽이 이 레코드를 공유한다.
 */
public record AuthErrorResponse(String code, String message) {

    public static AuthErrorResponse of(AuthErrorCode errorCode) {
        return new AuthErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}
