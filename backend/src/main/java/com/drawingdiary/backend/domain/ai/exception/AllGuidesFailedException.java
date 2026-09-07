package com.drawingdiary.backend.domain.ai.exception;

/**
 * 세 스타일이 <b>전부</b> 실패했을 때. 일부만 실패한 경우는 예외가 아니라 응답 안에
 * error로 담아 내려주므로(프론트가 성공한 것만 먼저 보여줄 수 있게), 이 예외는
 * 보여줄 게 하나도 없는 경우에만 던진다.
 */
public class AllGuidesFailedException extends RuntimeException {

    public AllGuidesFailedException(String detail) {
        super("선화 가이드를 생성하지 못했습니다. " + detail);
    }
}
