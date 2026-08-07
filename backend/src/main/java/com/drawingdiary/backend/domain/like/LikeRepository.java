package com.drawingdiary.backend.domain.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeRepository extends JpaRepository<Like, Long> {

    long deleteByDiaryId(Long diaryId);

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
