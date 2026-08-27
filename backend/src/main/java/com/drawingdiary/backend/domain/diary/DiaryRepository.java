package com.drawingdiary.backend.domain.diary;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * <ul>
     *   <li>작성자(= 협업자 중 첫 번째)가 :authorIds에 있어야 한다 — 팔로우한 사람의 글만.</li>
     *   <li>PUBLIC·FOLLOWERS_ONLY는 그대로 통과. 요청자가 작성자를 팔로우한다는 것이 이미
     *       위 조건으로 보장되므로 FOLLOWERS_ONLY의 팔로우 검사를 따로 할 필요가 없다.</li>
     *   <li>PRIVATE은 요청자가 협업자일 때만.</li>
     * </ul>
     * 팔로우 여부를 IN 절 하나로 넘기고 협업자·작성자 판정을 exists로 처리해서, 결과가 몇 건이든
     * 이 메서드는 쿼리 한 번이다.
     * <p>
     * min() 서브쿼리에서 {@code firstUser.deletedAt is null}을 직접 쓰는 이유: 이 조건은
     * User의 @SQLRestriction과 겹쳐 얼핏 군더더기로 보이지만, 조인을 <b>참조</b>하지 않으면
     * Hibernate가 쓰이지 않는 조인이라며 통째로 지워버린다(그러면 @SQLRestriction도 함께 사라진다).
     * 그 상태에서 방장이 탈퇴하면 min()이 탈퇴한 사람의 행을 집어 바깥 조건과 어긋나고, 결국
     * 남은 협업자를 팔로우하고 있어도 그 일기가 피드에서 통째로 사라진다.
     * findWithUserByDiaryIds가 정하는 작성자와 같은 사람을 골라야 한다.
     */
    @Query("""
            select d from Diary d
            where d.id < :cursor
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
              and (
                  d.visibility <> com.drawingdiary.backend.domain.diary.Visibility.PRIVATE
                  or exists (
                      select 1 from DiaryCollaborator me
                      where me.diary = d and me.user.id = :userId
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
}
