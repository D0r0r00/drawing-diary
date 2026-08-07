package com.drawingdiary.backend.domain.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long deleteByDiaryId(Long diaryId);

    /**
     * 목록 응답에 nickname이 필요한데 user가 LAZY라 그냥 조회하면 댓글 수만큼 추가 쿼리가 나간다.
     * join fetch는 inner join이라 User의 @SQLRestriction이 여기서도 걸리고, 그래서 탈퇴한
     * 계정이 쓴 댓글은 목록에서 빠진다 — 일기 목록의 작성자 처리와 같은 규칙이다.
     */
    @Query("select c from Comment c join fetch c.user where c.diary.id = :diaryId order by c.createdAt asc, c.id asc")
    List<Comment> findWithUserByDiaryId(@Param("diaryId") Long diaryId);
}
