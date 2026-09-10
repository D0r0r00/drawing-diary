package com.drawingdiary.backend.domain.user.exception;

/**
 * 활동 잔디의 year/month가 달력에 없는 값일 때. 400으로 돌려주는 이유는 조용히 보정하면
 * 프론트가 요청한 달과 다른 달의 잔디를 그리게 되기 때문이다 — 피드의 limit처럼
 * "값 하나 때문에 화면이 비는 것보다 낫다"가 성립하지 않는 자리다.
 */
public class InvalidActivityPeriodException extends RuntimeException {

    public InvalidActivityPeriodException(String message) {
        super(message);
    }
}
