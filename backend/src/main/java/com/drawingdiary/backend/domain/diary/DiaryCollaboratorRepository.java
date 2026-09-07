package com.drawingdiary.backend.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiaryCollaboratorRepository extends JpaRepository<DiaryCollaborator, Long> {

    boolean existsByDiaryIdAndUserId(Long diaryId, Long userId);

    long deleteByDiaryId(Long diaryId);

    /**
     * category까지 함께 가져온다. 응답에 categoryName이 들어가는데 Diary.category가
     * LAZY라, 조인 없이 두면 일기 건수만큼 카테고리 조회가 따라붙는다.
     */
    @Query("select d from DiaryCollaborator c join c.diary d left join fetch d.category "
            + "where c.user.id = :userId order by d.id desc")
    List<Diary> findDiariesByUserId(@Param("userId") Long userId);

    /**
     * Fetched for a whole page of diaries at once so the list endpoint stays at two
     * queries instead of one per diary. The join fetch on user is an inner join, so
     * @SQLRestriction drops withdrawn accounts here — the caller treats the first
     * surviving row per diary as the author.
     */
    @Query("select c from DiaryCollaborator c join fetch c.user where c.diary.id in :diaryIds order by c.id asc")
    List<DiaryCollaborator> findWithUserByDiaryIds(@Param("diaryIds") List<Long> diaryIds);
}
