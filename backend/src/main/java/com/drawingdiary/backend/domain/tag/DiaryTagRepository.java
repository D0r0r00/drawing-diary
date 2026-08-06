package com.drawingdiary.backend.domain.tag;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, Long> {

    long deleteByDiaryId(Long diaryId);
}
