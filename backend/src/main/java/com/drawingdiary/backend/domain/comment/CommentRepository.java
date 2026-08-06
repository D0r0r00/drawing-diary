package com.drawingdiary.backend.domain.comment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long deleteByDiaryId(Long diaryId);
}
