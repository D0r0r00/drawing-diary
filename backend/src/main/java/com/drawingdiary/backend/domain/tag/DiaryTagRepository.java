package com.drawingdiary.backend.domain.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, Long> {

    long deleteByDiaryId(Long diaryId);

    /**
     * 일기 여러 건의 태그를 한 번에. 피드·탐색·랜덤 추천이 카드마다 태그를 조회하면
     * 건수만큼 쿼리가 늘어나는 자리라, 목록 경로는 반드시 이걸 통해야 한다.
     *
     * <p>엔티티 대신 프로젝션인 이유: DiaryTag.diary가 LAZY라 엔티티로 받으면 diaryId를
     * 꺼내는 것만으로 프록시가 초기화될 수 있다. dt.diary.id는 FK 컬럼이라 조인 없이 읽히므로
     * 이 쿼리는 어떤 경우에도 한 번으로 끝난다.
     *
     * <p>정렬은 태그 이름 오름차순(구체적인 자모·알파벳 순서는 DB 콜레이션을 따른다). 붙인
     * 순서를 보존하지 않는 대신 같은 일기를 언제 조회해도 태그 순서가 같아, 프론트가 카드를
     * 다시 그릴 때 순서가 흔들리지 않는다. 쓰기 경로도 저장 후 이 조회를 다시 타므로
     * 수정 응답과 상세 조회의 순서가 일치한다(TagService.replaceTags).
     */
    @Query("""
            select dt.diary.id as diaryId, t.id as tagId, t.name as name
            from DiaryTag dt
            join dt.tag t
            where dt.diary.id in :diaryIds
            order by t.name asc
            """)
    List<DiaryTagRow> findTagRowsByDiaryIds(@Param("diaryIds") List<Long> diaryIds);

    interface DiaryTagRow {
        Long getDiaryId();

        Long getTagId();

        String getName();
    }
}
