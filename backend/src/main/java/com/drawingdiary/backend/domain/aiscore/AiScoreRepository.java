package com.drawingdiary.backend.domain.aiscore;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiScoreRepository extends JpaRepository<AiScore, Long> {

    long deleteByDiaryId(Long diaryId);
}
