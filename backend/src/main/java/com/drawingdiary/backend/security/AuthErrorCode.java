package com.drawingdiary.backend.security;

import lombok.Getter;

/**
 * 인증 실패 사유. 프론트가 분기하는 값이므로 이름(enum name)이 그대로 응답의 code가 된다.
 *
 * 만료(TOKEN_EXPIRED)와 그 외 무효(TOKEN_INVALID)를 나누는 것이 핵심이다. 만료는
 * refresh로 회복 가능한 상태이고, 나머지는 재로그인 외에는 방법이 없다. 서명 오류·형식
 * 오류·토큰 타입 오용을 굳이 더 쪼개지 않는 이유는, 어느 쪽이든 클라이언트가 할 수 있는
 * 일이 같고 공격자에게 실패 원인을 상세히 알려줄 이유도 없기 때문이다.
 */
@Getter
public enum AuthErrorCode {

    TOKEN_MISSING("인증이 필요합니다"),
    TOKEN_EXPIRED("토큰이 만료되었습니다"),
    TOKEN_INVALID("유효하지 않은 토큰입니다");

    private final String message;

    AuthErrorCode(String message) {
        this.message = message;
    }
}
