package com.drawingdiary.backend.domain.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drawingdiary.backend.domain.user.User;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {

    long deleteByDiaryId(Long diaryId);

    /**
     * 좋아요를 누른 사람 목록. Like 행이 아니라 User를 select하는 것이 핵심이다 —
     * 그래야 조인이 실제로 걸려 User의 @SQLRestriction이 적용되고, 탈퇴한 계정이
     * 목록에서 빠진다. FollowRepository.findFollowersByFollowingId와 같은 이유이자
     * 같은 모양이다.
     *
     * <p>그래서 이 목록의 길이는 countByDiaryId(= likes 행 수)보다 작을 수 있다.
     * 좋아요 <b>수</b>는 누른 기록 그대로여야 랭킹 점수가 흔들리지 않고, 목록은 지금
     * 남아 있는 사람만 보여주는 게 맞아서 둘을 일부러 다르게 뒀다.
     *
     * <p>최신순이다. 좋아요는 취소·재등록이 가능해 like_id가 곧 최근에 누른 순서다.
     */
    @Query("select l.user from Like l where l.diary.id = :diaryId order by l.id desc")
    List<User> findUsersByDiaryId(@Param("diaryId") Long diaryId);

    long deleteByDiaryIdAndUserId(Long diaryId, Long userId);

    long countByDiaryId(Long diaryId);

    /**
     * 좋아요 등록은 멱등이어야 한다. "있는지 확인 후 저장"으로 짜면 버튼을 연타해 두 요청이
     * 겹쳤을 때 UNIQUE(diary_id, user_id) 위반이 나는데, 그 예외를 잡아도 이미 영속성 컨텍스트와
     * 트랜잭션이 롤백 대상으로 마킹된 뒤라 이어서 likeCount를 셀 수 없다. ON CONFLICT DO NOTHING은
     * 예외 없이 한 쿼리로 끝나므로 그 뒤의 count가 그대로 유효하다.
     *
     * created_at을 명시하는 이유는 컬럼이 NOT NULL인데 이 경로가 @CreationTimestamp를 타지 않기 때문.
     */
    @Modifying
    @Query(
            value = """
                    insert into likes (diary_id, user_id, created_at)
                    values (:diaryId, :userId, current_timestamp)
                    on conflict (diary_id, user_id) do nothing
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(@Param("diaryId") Long diaryId, @Param("userId") Long userId);
}
