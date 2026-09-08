package com.drawingdiary.backend.domain.diary;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    /**
     * deprecated된 /api/diaries와 /api/explore가 함께 쓴다. 커서는 "이 id보다 작은 것"이라
     * 첫 페이지는 null 대신 Long.MAX_VALUE를 넘긴다 — 파라미터가 항상 값을 가지면
     * `:cursor is null or ...` 같은 분기가 사라져 쿼리와 실행 계획이 단순해진다.
     * 전체를 받고 싶으면 Pageable.unpaged()를 넘긴다.
     */
    @Query("""
            select d from Diary d
            left join fetch d.category
            where d.visibility = :visibility
              and d.id < :cursor
            order by d.id desc
            """)
    List<Diary> findByVisibilityBefore(
            @Param("visibility") Visibility visibility,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * 팔로잉 피드. DiaryService.canRead와 같은 판정을 목록에 한 번에 적용한 것으로,
     * 둘 중 하나만 고치면 "일기는 안 보이는데 피드에는 뜨는" 어긋남이 생기니 같이 봐야 한다.
     *
     * <h4>포함 조건 — 협업자 중 누구라도 팔로우 중이면</h4>
     * 예전에는 "작성자(= 첫 협업자)를 팔로우한 일기"만 넣었다. 같이 그린 사람을 팔로우하고 있어도
     * 그 사람이 방장이 아니면 피드에 안 뜨는 게 어색해서, 협업자 아무나 한 명이라도 :authorIds에
     * 있으면 포함하도록 넓혔다. exists라 협업자 여럿을 동시에 팔로우해도 일기는 한 번만 나온다
     * (distinct 없이 중복 제거되고, 커서 페이지네이션도 그대로 성립).
     *
     * <h4>공개 범위 판정은 넓히지 않았다</h4>
     * 포함 조건만 넓히고 FOLLOWERS_ONLY 규칙은 canRead와 똑같이 "작성자를 팔로우"로 남겨뒀다.
     * 여기까지 넓히면 작성자가 모르는 사람(공동 작업자의 팔로워)에게 글이 열리는 셈이라,
     * 작성자가 고른 공개 범위를 서버가 임의로 완화하는 일이 된다. 덕분에 피드에 뜬 일기는
     * 항상 탭해서 열 수 있다(피드 ⊆ 조회 가능).
     *
     * <h4>min() 서브쿼리의 firstUser.deletedAt</h4>
     * User의 @SQLRestriction과 겹쳐 얼핏 군더더기로 보이지만, 조인을 <b>참조</b>하지 않으면
     * Hibernate가 쓰이지 않는 조인이라며 통째로 지워버린다(그러면 @SQLRestriction도 함께 사라진다).
     * 그 상태에서 방장이 탈퇴하면 min()이 탈퇴한 사람의 행을 집어 바깥 조건과 어긋난다.
     * findWithUserByDiaryIds가 정하는 작성자와 같은 사람을 골라야 한다.
     * <p>
     * 이제 이 서브쿼리는 FOLLOWERS_ONLY 가지에서만 쓰인다. PUBLIC 일기는 방장이 탈퇴해도
     * 남은 협업자를 팔로우하고 있으면 그대로 보이고, 응답의 작성자는 다음 생존 협업자가 된다.
     * <p>
     * 팔로우 여부를 IN 절 하나로 넘기고 협업자·작성자 판정을 exists로 처리해서, 결과가 몇 건이든
     * 이 메서드는 쿼리 한 번이다.
     */
    @Query("""
            select d from Diary d
            left join fetch d.category
            where d.id < :cursor
              and exists (
                  select 1 from DiaryCollaborator mate
                  where mate.diary = d and mate.user.id in :authorIds
              )
              and (
                  d.visibility = com.drawingdiary.backend.domain.diary.Visibility.PUBLIC
                  or exists (
                      select 1 from DiaryCollaborator me
                      where me.diary = d and me.user.id = :userId
                  )
                  or (
                      d.visibility = com.drawingdiary.backend.domain.diary.Visibility.FOLLOWERS_ONLY
                      and exists (
                          select 1 from DiaryCollaborator author
                          join author.user authorUser
                          where author.diary = d
                            and authorUser.id in :authorIds
                            and author.id = (
                                select min(first.id) from DiaryCollaborator first
                                join first.user firstUser
                                where first.diary = d
                                  and firstUser.deletedAt is null
                            )
                      )
                  )
              )
            order by d.id desc
            """)
    List<Diary> findFeedByAuthorIds(
            @Param("userId") Long userId,
            @Param("authorIds") List<Long> authorIds,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * 홈 화면 랜덤 추천이 뽑는 PUBLIC 일기 id. 정렬만 다를 뿐 대상 집합은
     * findByVisibilityBefore와 같다(PUBLIC 전체).
     *
     * <h4>왜 엔티티가 아니라 id인가</h4>
     * JPQL에는 표준 random 함수가 없어 네이티브 SQL이어야 하는데, 네이티브로 Diary를 그대로
     * 받으면 category가 지연 로딩이라 카드마다 조회가 한 번씩 더 나간다(N+1). id만 먼저 뽑고
     * findWithCategoryByIds로 한 번에 채우면 건수와 무관하게 쿼리 두 번으로 끝난다.
     *
     * <h4>⚠️ ORDER BY RANDOM()의 비용</h4>
     * 조건에 맞는 행 <b>전체</b>에 난수를 매겨 정렬한 뒤 앞에서 자르므로, 인덱스가 있어도
     * 매번 풀 스캔이다. 지금 규모(수천 건)에서는 체감되지 않지만 일기가 수십만 건이 되면
     * 눈에 띄게 느려진다. 그 시점에는 TABLESAMPLE이나 난수 키 컬럼처럼 전체를 훑지 않는
     * 방식으로 갈아타야 한다.
     */
    @Query(value = """
            select d.diary_id from diaries d
            where d.visibility = :visibility
            order by random()
            """, nativeQuery = true)
    List<Long> findRandomIdsByVisibility(
            @Param("visibility") String visibility,
            Pageable pageable
    );

    /**
     * 랜덤 추천이 id 목록을 카드로 바꿀 때 쓴다. 목록 경로들과 같은 fetch join이라
     * categoryId·categoryName을 읽어도 추가 쿼리가 나가지 않는다.
     *
     * in 절은 순서를 보장하지 않으므로, 뽑은 순서를 지켜야 하는 호출자가 직접 재정렬한다.
     */
    @Query("""
            select d from Diary d
            left join fetch d.category
            where d.id in :ids
            """)
    List<Diary> findWithCategoryByIds(@Param("ids") List<Long> ids);

    /**
     * 카테고리 삭제 전에 참조를 끊는다. diaries.category_id에 ON DELETE SET NULL이 없어서
     * 이 단계를 건너뛰면 FK 위반이 난다.
     *
     * 영속성 컨텍스트를 우회하는 벌크 연산이라, 이미 로딩된 Diary가 남아 있으면 옛 카테고리를
     * 그대로 들고 있게 된다. clearAutomatically로 컨텍스트를 비워 그 불일치를 막는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Diary d set d.category = null where d.category.id = :categoryId")
    int clearCategory(@Param("categoryId") Long categoryId);
}
