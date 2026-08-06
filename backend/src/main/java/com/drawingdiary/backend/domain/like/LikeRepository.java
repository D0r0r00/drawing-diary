package com.drawingdiary.backend.domain.like;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {

    long deleteByDiaryId(Long diaryId);
}
