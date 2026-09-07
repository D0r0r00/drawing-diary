package com.drawingdiary.backend.domain.ai.exception;

/**
 * AI 서버 호출이 왜 실패했는지. 로그와 에러 메시지에서 "그냥 실패"와 구분하기 위한 것으로,
 * 대응이 서로 다르다 — TIMEOUT은 재시도할 만하고, UNAVAILABLE은 서버 상태를 봐야 하고,
 * INVALID_RESPONSE는 우리 파싱 코드나 AI 서버 스펙이 어긋난 것이라 코드를 고쳐야 한다.
 */
public enum AiFailureReason {

    /** 연결은 됐지만 read timeout 안에 응답이 오지 않음. Render가 깨어나는 중일 수 있다. */
    TIMEOUT("AI 서버 응답이 시간 안에 오지 않았습니다"),

    /** 연결 자체가 안 되거나 5xx. 서버가 내려갔거나 배포 중. */
    UNAVAILABLE("AI 서버에 연결할 수 없습니다"),

    /** 4xx. 우리가 보낸 요청이 AI 서버 스펙과 맞지 않음. */
    BAD_REQUEST("AI 서버가 요청을 거부했습니다"),

    /** 200인데 본문이 기대한 형식이 아님(JSON 파싱 실패, 이미지가 아닌 바이트 등). */
    INVALID_RESPONSE("AI 서버 응답 형식이 올바르지 않습니다");

    private final String message;

    AiFailureReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
