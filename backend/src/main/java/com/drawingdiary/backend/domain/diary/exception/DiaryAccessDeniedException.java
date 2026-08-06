package com.drawingdiary.backend.domain.diary.exception;

public class DiaryAccessDeniedException extends RuntimeException {

    public DiaryAccessDeniedException(Long diaryId) {
        super("비공개 일기입니다: " + diaryId);
    }
}
