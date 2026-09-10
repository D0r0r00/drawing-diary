package com.drawingdiary.backend.domain.aiscore;

import com.drawingdiary.backend.domain.aiscore.dto.AiScoreResponse;
import com.drawingdiary.backend.domain.aiscore.dto.AiScoreSaveRequest;
import com.drawingdiary.backend.domain.aiscore.dto.MyRankingItemResponse;
import com.drawingdiary.backend.domain.aiscore.dto.RankingItemResponse;
import com.drawingdiary.backend.domain.aiscore.exception.AiScoreNotFoundException;
import com.drawingdiary.backend.domain.diary.Diary;
import com.drawingdiary.backend.domain.diary.DiaryRepository;
import com.drawingdiary.backend.domain.diary.DiaryService;
import com.drawingdiary.backend.domain.like.LikeRepository;
import com.drawingdiary.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiScoreService {

    private final AiScoreRepository aiScoreRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryService diaryService;
    private final LikeRepository likeRepository;

    private static final int DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 50;

    /**
     * AI 서버가 매긴 두 점수를 저장하고 총점을 계산한다. 일기와 1:1이라 이미 점수가 있으면
     * 새 행을 만들지 않고 덮어쓴다.
     *
     * 좋아요 점수는 저장 시점의 좋아요 수로 계산한다. 이후 좋아요가 변하면
     * LikeService가 refreshLikeScore로 총점만 다시 맞춘다.
     */
    @Transactional
    public AiScoreResponse save(Long userId, Long diaryId, AiScoreSaveRequest request) {
        return save(userId, diaryId, request, null);
    }

    /**
     * 점수와 평가 코멘트를 한 트랜잭션에서 저장한다. AI 자동 산정만 feedback을 넘기고,
     * 수동 저장(POST .../scores)은 바디에 코멘트가 없으므로 null을 넘겨 <b>기존 값을 유지</b>한다
     * — 수동으로 점수를 다시 매겼다고 AI가 남긴 코멘트를 지울 이유가 없다.
     *
     * @param feedback null이거나 공백이면 기존 ai_comment를 그대로 둔다.
     */
    @Transactional
    public AiScoreResponse save(Long userId, Long diaryId, AiScoreSaveRequest request, String feedback) {
        // 볼 수 없는 일기에는 점수도 매길 수 없다. 좋아요와 같은 판정을 쓴다.
        Diary diary = diaryService.getReadableDiaryOrThrow(userId, diaryId);

        int likeScore = ScoreCalculator.likeScore(likeRepository.countByDiaryId(diaryId));
        AiScore score = aiScoreRepository.findByDiaryId(diaryId)
                .orElseGet(() -> aiScoreRepository.save(AiScore.builder()
                        .diary(diary)
                        // 아래 applyAiScores가 바로 덮어쓰지만, NOT NULL 컬럼이라 생성 시점에
                        // 값이 있어야 한다. themeScore는 통합 랭킹으로 쓰지 않게 되어 계속 0이다.
                        .relevanceScore(0)
                        .colorScore(0)
                        .themeScore(0)
                        .totalScore(0)
                        .build()));

        score.applyAiScores(request.relevanceScore(), request.colorScore(), likeScore);
        if (feedback != null && !feedback.isBlank()) {
            score.applyFeedback(feedback);
        }
        aiScoreRepository.flush();

        return new AiScoreResponse(
                diaryId, score.getRelevanceScore(), score.getColorScore(), likeScore, score.getTotalScore(),
                feedbackOrNull(score.getAiComment()));
    }

    @Transactional(readOnly = true)
    public AiScoreResponse find(Long userId, Long diaryId) {
        diaryService.getReadableDiaryOrThrow(userId, diaryId);

        AiScore score = aiScoreRepository.findByDiaryId(diaryId)
                .orElseThrow(() -> new AiScoreNotFoundException(diaryId));

        return new AiScoreResponse(
                diaryId,
                score.getRelevanceScore(),
                score.getColorScore(),
                ScoreCalculator.likeScore(likeRepository.countByDiaryId(diaryId)),
                score.getTotalScore(),
                feedbackOrNull(score.getAiComment()));
    }

    /**
     * 좋아요가 실제로 늘거나 줄었을 때만 부른다. 점수가 아직 없는 일기는 랭킹에 없으므로
     * 아무것도 하지 않는다 — 좋아요만으로 점수 행이 생기면 AI 점수가 0인 일기가 랭킹에 섞인다.
     */
    @Transactional
    public void refreshLikeScore(Long diaryId) {
        aiScoreRepository.findByDiaryId(diaryId).ifPresent(score ->
                score.applyLikeScore(ScoreCalculator.likeScore(likeRepository.countByDiaryId(diaryId))));
    }

    /**
     * 전체 랭킹 한 페이지. 순위 목록을 먼저 뽑고(1쿼리), 그 id들로 일기 본문(1쿼리)과
     * 작성자(1쿼리)를 한 번에 채운다 — 페이지 크기와 무관하게 총 3쿼리다.
     */
    @Transactional(readOnly = true)
    public List<RankingItemResponse> findRanking(int offset, int limit) {
        return toRankingItems(
                aiScoreRepository.findRankingPage(pageSize(limit), offsetOrZero(offset)),
                offsetOrZero(offset));
    }

    /**
     * 친구 랭킹 — 내가 팔로우하는 사람이 참여한 일기만. 전체 랭킹과 조회·조립·페이지네이션이
     * 모두 같고 대상 집합만 좁다.
     *
     * <p><b>rank는 이 목록 안에서의 순위</b>다. 전체 랭킹에서 몇 위인지가 아니다
     * (그건 findMyRanking). 이유는 RankingController.friendRanking 주석 참고.
     */
    @Transactional(readOnly = true)
    public List<RankingItemResponse> findFriendRanking(Long userId, int offset, int limit) {
        return toRankingItems(
                aiScoreRepository.findFriendRankingPage(userId, pageSize(limit), offsetOrZero(offset)),
                offsetOrZero(offset));
    }

    /**
     * 순위 행(diaryId, totalScore)에 제목·썸네일·작성자를 채운다. 전체 랭킹과 친구 랭킹이
     * 공유하므로 두 화면의 카드가 어긋나지 않는다.
     *
     * <p>id 목록으로 일기(1쿼리)와 작성자(1쿼리)를 한 번에 가져온다 — 페이지 크기와 무관하게
     * 순위 조회까지 합쳐 총 3쿼리다.
     *
     * @param offset 이 페이지가 시작하는 위치. rank = offset + 순번이라 페이지를 넘겨도
     *               번호가 이어진다.
     */
    private List<RankingItemResponse> toRankingItems(List<AiScoreRepository.RankingRow> rows, int offset) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> diaryIds = rows.stream().map(AiScoreRepository.RankingRow::getDiaryId).toList();
        Map<Long, Diary> diaries = diaryRepository.findAllById(diaryIds).stream()
                .collect(Collectors.toMap(Diary::getId, Function.identity()));
        Map<Long, User> authors = diaryService.findAuthorsByDiaryIds(diaryIds);

        int startRank = offset + 1;
        return IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    AiScoreRepository.RankingRow row = rows.get(index);
                    Diary diary = diaries.get(row.getDiaryId());
                    User author = authors.get(row.getDiaryId());
                    return new RankingItemResponse(
                            startRank + index,
                            row.getDiaryId(),
                            diary == null ? null : diary.getTitle(),
                            diary == null ? null : diary.getFinalImgUrl(),
                            row.getTotalScore(),
                            author == null ? null : author.getId(),
                            author == null ? null : author.getNickname()
                    );
                })
                .toList();
    }

    /**
     * 내가 참여한 일기들의 전체 순위. 점수가 있는 PUBLIC 일기만 대상이라 해당 없으면 빈 배열이다.
     */
    @Transactional(readOnly = true)
    public List<MyRankingItemResponse> findMyRanking(Long userId) {
        return aiScoreRepository.findMyRanking(userId).stream()
                .map(row -> new MyRankingItemResponse(row.getDiaryId(), row.getRank(), row.getTotalScore()))
                .toList();
    }

    /**
     * ai_comment는 NULL일 수도, 빈 문자열일 수도 있다(AI 서버가 공백을 주는 경우).
     * 프론트가 "코멘트 없음"을 한 가지 방식으로만 판정하도록 응답에서는 둘 다 null로 맞춘다.
     */
    private String feedbackOrNull(String aiComment) {
        return aiComment == null || aiComment.isBlank() ? null : aiComment;
    }

    /**
     * 피드와 마찬가지로 잘못된 값에 에러를 내기보다 조용히 보정한다.
     */
    private int pageSize(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int offsetOrZero(int offset) {
        return Math.max(offset, 0);
    }
}
