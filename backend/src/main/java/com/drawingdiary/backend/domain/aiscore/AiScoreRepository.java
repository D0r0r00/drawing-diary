package com.drawingdiary.backend.domain.aiscore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiScoreRepository extends JpaRepository<AiScore, Long> {

    long deleteByDiaryId(Long diaryId);

    Optional<AiScore> findByDiaryId(Long diaryId);

    /**
     * 랭킹 한 페이지. 정렬은 total_score 내림차순, 동점이면 diary_id 오름차순 — 두 컬럼을
     * 합치면 전순서라 같은 데이터에 대해 순위가 요청마다 달라지지 않는다.
     * <p>
     * 커서 대신 offset을 쓴다. 피드는 diary_id 하나로 자를 수 있지만 랭킹의 정렬 키는
     * (total_score, diary_id) 두 개라 diary_id 커서가 그대로 맞지 않고, 무엇보다 화면에
     * 표시할 rank가 "몇 번째부터인지"를 알아야 나온다. offset이면 rank = offset + 순번으로
     * 정확히 떨어진다. 스크롤 도중 점수가 바뀌면 항목이 밀려 중복·누락이 생길 수 있는데,
     * 랭킹은 AI 점수 저장과 좋아요 변동에만 움직여 피드만큼 자주 변하지 않는다.
     * <p>
     * 엔티티 대신 id와 점수만 뽑는다. 제목·썸네일·작성자는 호출자가 한 번에 모아 채우므로
     * 여기서 조인을 늘리면 페이지 크기만큼 중복 행만 늘어난다.
     */
    @Query(
            value = """
                    select a.diary_id as diaryId, a.total_score as totalScore
                    from ai_scores a
                    join diaries d on d.diary_id = a.diary_id
                    where d.visibility = 'PUBLIC'
                    order by a.total_score desc, a.diary_id asc
                    limit :limit offset :offset
                    """,
            nativeQuery = true
    )
    List<RankingRow> findRankingPage(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 친구 랭킹 한 페이지 — 내가 팔로우하는 사람이 <b>협업자로 참여한</b> 일기.
     *
     * <p>정렬·대상(PUBLIC)·페이지네이션이 findRankingPage와 같고 exists 조건만 더 붙는다.
     * "협업자 중 누구라도"인 것은 findFeedByAuthorIds가 피드를 넓힌 것과 같은 판단이다:
     * 같이 그린 사람을 팔로우하고 있으면 그 일기도 내 친구들의 결과물이다. exists라
     * 한 일기에서 여러 명을 팔로우 중이어도 행이 중복되지 않아 offset이 그대로 성립한다.
     *
     * <p>내 일기는 여기 나오지 않는다 — 자기 자신은 팔로우할 수 없기 때문이다
     * (내 순위는 /api/rankings/me).
     *
     * <p><b>rank는 이 목록 안에서의 순위다</b>(전체 랭킹에서의 순위가 아니다).
     * findMyRanking과 의미가 다르니 주의. 이유는 RankingController 주석 참고.
     */
    @Query(
            value = """
                    select a.diary_id as diaryId, a.total_score as totalScore
                    from ai_scores a
                    join diaries d on d.diary_id = a.diary_id
                    where d.visibility = 'PUBLIC'
                      and exists (
                          select 1
                          from diary_collaborators dc
                          join follows f on f.following_id = dc.user_id
                          where dc.diary_id = a.diary_id
                            and f.follower_id = :userId
                      )
                    order by a.total_score desc, a.diary_id asc
                    limit :limit offset :offset
                    """,
            nativeQuery = true
    )
    List<RankingRow> findFriendRankingPage(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 내가 협업자인 일기들의 <b>전체 랭킹에서의</b> 순위. 순위를 매긴 뒤에 내 것만 걸러야 하므로
     * 서브쿼리로 전체 순위를 먼저 계산한다 — 내 일기만 놓고 세면 항상 1위부터 나온다.
     * <p>
     * rank()가 아니라 row_number()인 이유: 정렬 키가 (total_score, diary_id) 전순서라 동점이
     * 존재하지 않고, rank()를 쓰면 같은 결과에 이름만 다른 함수가 된다. row_number()가
     * "몇 번째"라는 의미를 그대로 드러낸다.
     * <p>
     * 전체 랭킹이 PUBLIC만 대상이라 여기서도 PUBLIC만 센다. 비공개 일기는 공개 랭킹에
     * 자리를 가질 수 없어 점수가 있어도 이 목록에 나오지 않는다.
     */
    @Query(
            value = """
                    select r.diary_id as diaryId, r.total_score as totalScore, r.position as rank
                    from (
                        select a.diary_id,
                               a.total_score,
                               row_number() over (order by a.total_score desc, a.diary_id asc) as position
                        from ai_scores a
                        join diaries d on d.diary_id = a.diary_id
                        where d.visibility = 'PUBLIC'
                    ) r
                    join diary_collaborators c on c.diary_id = r.diary_id
                    where c.user_id = :userId
                    order by r.position asc
                    """,
            nativeQuery = true
    )
    List<MyRankingRow> findMyRanking(@Param("userId") Long userId);

    interface RankingRow {
        Long getDiaryId();

        Integer getTotalScore();
    }

    interface MyRankingRow {
        Long getDiaryId();

        Integer getTotalScore();

        Integer getRank();
    }
}
