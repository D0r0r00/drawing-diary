package com.drawingdiary.backend.domain.tag;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 태그 검색. 저장된 이름은 대소문자를 그대로 두고 비교할 때만 양쪽을 lower()로 맞춘다.
     *
     * <p>escape가 필요한 이유: 검색어에 들어 있는 %와 _는 LIKE의 와일드카드라, 그대로 두면
     * "50%"가 전체 태그를 긁어온다. 호출자(TagService.likePattern)가 이 문자들 앞에 !를 붙여
     * 넘기고 여기서 escape로 알려준다.
     *
     * <p>흔한 \ 대신 !인 이유: HQL 문자열 리터럴에서 백슬래시가 escape 문자로 먼저 해석돼
     * escape '\' 를 한 글자로 읽지 못한다(SemanticException). !는 그런 충돌이 없다.
     */
    @Query("""
            select t from Tag t
            where lower(t.name) like lower(:pattern) escape '!'
            order by t.name asc
            """)
    List<Tag> search(@Param("pattern") String pattern, Pageable pageable);

    /**
     * 이름으로 기존 태그를 찾는다. 대소문자를 무시해서 "Cat"과 "cat"이 같은 태그로 묶인다
     * — tags.name의 UNIQUE는 대소문자를 구분하므로, 이 조회 없이 저장하면 사실상 같은
     * 태그가 철자만 다른 행으로 늘어난다.
     */
    @Query("select t from Tag t where lower(t.name) in :lowerNames")
    List<Tag> findByLowerNameIn(@Param("lowerNames") List<String> lowerNames);

    /**
     * 없을 때만 만든다. 조회 후 저장하는 방식은 같은 이름을 동시에 처음 다는 두 요청이
     * 모두 조회를 통과해 UNIQUE 위반(500)을 낼 수 있는데, ON CONFLICT면 DB가 그 경합을
     * 흡수한다 — 진 쪽은 조용히 아무것도 하지 않고, 뒤이은 재조회에서 이긴 쪽 행을 같이 쓴다.
     *
     * <p>Postgres 전용 구문이다. 이 프로젝트는 이미 랭킹·랜덤 추천에서 Postgres 전용 SQL을
     * 쓰고 있어 새로 생기는 제약은 없다.
     */
    @Modifying
    @Query(value = "insert into tags (name) values (:name) on conflict (name) do nothing", nativeQuery = true)
    void insertIfAbsent(@Param("name") String name);
}
