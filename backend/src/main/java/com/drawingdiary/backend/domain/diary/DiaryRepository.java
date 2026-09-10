package com.drawingdiary.backend.domain.diary;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    /**
     * DiaryService.canRead를 SQL로 옮긴 것. 여러 건을 한 번에 판정해야 하는 경로가 쓴다 —
     * 목록마다 canRead를 부르면 협업자·팔로우 확인이 건수만큼 반복된다(N+1).
     *
     * <p>상수로 뽑아 @Query 문자열에 이어 붙이는 이유는 <b>갈라지지 않게</b> 하기 위해서다.
     * 타인 일기 목록과 활동 잔디가 같은 판정을 쓰는데, 각자 SQL을 적어두면 한쪽만 고쳤을 때
     * "잔디에는 찍혔는데 목록에는 없는 날"이 생긴다. 자바의 상수 문자열은 컴파일 타임에
     * 합쳐지므로 애노테이션 값으로 쓸 수 있고, 한 곳만 고치면 두 쿼리가 함께 따라온다.
     *
     * <p>바인딩이 필요한 이름은 <b>:requesterId</b> 하나다. 이 상수를 쓰는 쿼리는 반드시
     * 그 이름의 파라미터를 받아야 한다.
     *
     * <h4>FOLLOWERS_ONLY는 "작성자를 팔로우"다 — 목록 주인이 아니라</h4>
     * 목록 주인이 방장이 아닌 협업자일 수 있는데, 그때 작성자는 제3자다. 판정을 "목록 주인을
     * 팔로우"로 넓히면 작성자를 팔로우하지 않은 사람에게도 일기가 목록에 뜨고, 정작 열면
     * canRead가 403을 낸다. findFeedByAuthorIds가 포함 조건만 넓히고 공개 범위 판정은
     * 넓히지 않은 것과 같은 이유다: 목록에 뜬 일기는 항상 열 수 있어야 한다.
     *
     * <h4>min() 서브쿼리의 firstUser.deletedAt</h4>
     * findFeedByAuthorIds의 같은 서브쿼리와 이유가 같다. 조인을 참조하지 않으면 Hibernate가
     * 쓰이지 않는 조인이라며 지워버리고, 그러면 User의 @SQLRestriction도 함께 사라져
     * 탈퇴한 방장이 작성자로 뽑힌다. findAuthors가 고르는 사람과 같아야 한다.
     */
    String VISIBLE_TO_REQUESTER = """
            (
                d.visibility = com.drawingdiary.backend.domain.diary.Visibility.PUBLIC
                or exists (
                    select 1 from DiaryCollaborator me
                    where me.diary = d and me.user.id = :requesterId
                )
                or (
                    d.visibility = com.drawingdiary.backend.domain.diary.Visibility.FOLLOWERS_ONLY
                    and exists (
                        select 1 from DiaryCollaborator author
                        join author.user authorUser
                        where author.diary = d
                          and author.id = (
                              select min(first.id) from DiaryCollaborator first
                              join first.user firstUser
                              where first.diary = d
                                and firstUser.deletedAt is null
                          )
                          and exists (
                              select 1 from Follow f
                              where f.follower.id = :requesterId
                                and f.following.id = authorUser.id
                          )
                    )
                )
            )
            """;

    /**
     * 프로필 주인이 협업자로 참여한 일기인지. 위 상수와 마찬가지로 이어 붙여 쓰며,
     * <b>:userId</b> 파라미터를 요구한다. 방장으로 한정하지 않는 이유는 같이 그린 일기도
     * 그 사람의 활동이기 때문이다(피드의 포함 조건과 같은 판단).
     */
    String TARGET_IS_COLLABORATOR = """
            exists (
                select 1 from DiaryCollaborator target
                where target.diary = d and target.user.id = :userId
            )
            """;

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

    /**
     * 타인 프로필의 일기 목록. 커서·정렬·fetch join이 findFeedByAuthorIds와 같고 대상 집합만
     * 다르다(팔로우한 사람들 → 프로필 주인 한 명). 응답 변환도 피드와 같은 toFeedItems를 타므로
     * 카드 모양이 두 화면에서 어긋나지 않는다.
     *
     * 권한 판정이 SQL 안에 있어 결과가 몇 건이든 쿼리는 이 한 번이다.
     */
    @Query("select d from Diary d "
            + "left join fetch d.category "
            + "where d.id < :cursor "
            + "  and " + TARGET_IS_COLLABORATOR
            + "  and " + VISIBLE_TO_REQUESTER
            + "order by d.id desc")
    List<Diary> findByCollaboratorVisibleTo(
            @Param("userId") Long userId,
            @Param("requesterId") Long requesterId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * 활동 잔디가 셀 대상의 작성 시각. 날짜별 집계를 SQL의 group by로 하지 않고 시각만 받아
     * 자바에서 묶는 이유는 두 가지다.
     *
     * <p>첫째, group by를 하려면 timestamp를 날짜로 자르는 함수가 필요한데 그건 방언에 걸린다.
     * 둘째이자 더 중요한 이유는, 같은 판정을 쓰는 쿼리가 하나라도 줄어야 목록과 잔디가
     * 어긋나지 않기 때문이다. 여기서 받은 일기는 위 findByCollaboratorVisibleTo가 내려주는
     * 것과 정확히 같은 집합이다.
     *
     * <p>범위가 한 달로 잘려 있어 행 수가 적다(한 사람이 한 달에 쓰는 일기 수). 페이지네이션
     * 없이 다 받아도 되는 크기이고, 쿼리는 한 번이다.
     *
     * @param from 그 달의 1일 00:00 (포함)
     * @param to   다음 달 1일 00:00 (제외) — 말일 23:59:59.999 로 자르면 경계에 걸리는 값이 샌다
     */
    @Query("select d.createdAt from Diary d "
            + "where " + TARGET_IS_COLLABORATOR
            + "  and " + VISIBLE_TO_REQUESTER
            + "  and d.createdAt >= :from and d.createdAt < :to")
    List<LocalDateTime> findVisibleCreatedAtByCollaborator(
            @Param("userId") Long userId,
            @Param("requesterId") Long requesterId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
