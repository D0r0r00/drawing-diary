package com.drawingdiary.backend.domain.diary.exception;

public class NotDiaryCollaboratorException extends RuntimeException {

    public NotDiaryCollaboratorException(Long diaryId) {
        super("일기를 함께 그린 사람만 수정하거나 삭제할 수 있습니다: " + diaryId);
    }
}
