package com.drawingdiary.backend.domain.aiscore.exception;

public class AiScoreNotFoundException extends RuntimeException {

    public AiScoreNotFoundException(Long diaryId) {
        super("일기의 AI 점수를 찾을 수 없습니다: " + diaryId);
    }
}
