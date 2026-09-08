package com.drawingdiary.backend.domain.tag;

import com.drawingdiary.backend.domain.diary.Diary;
import com.drawingdiary.backend.domain.tag.dto.TagResponse;
import com.drawingdiary.backend.domain.tag.exception.InvalidTagNameException;
import com.drawingdiary.backend.domain.tag.exception.TooManyTagsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 태그는 전역 사전(tags)과 일기별 연결(diary_tags)로 나뉜다. 이름은 전역에서 유일하고,
 * 같은 이름을 여러 일기가 달아도 늘어나는 행은 diary_tags 쪽뿐이다.
 *
 * <p>태그를 붙이는 경로는 발행(Room submit)과 수정(PATCH /api/diaries/{id}) 둘 다 여기를
 * 거친다. 이름 정규화·상한·upsert 규칙이 한 곳에만 있어야 두 경로가 어긋나지 않는다.
 */
@Service
@RequiredArgsConstructor
public class TagService {

    /**
     * tags.name이 varchar(50)이라 그 이상은 저장 자체가 안 된다. 조용히 자르면 사용자가
     * 입력한 것과 다른 태그가 달리므로 400으로 돌려준다.
     */
    private static final int MAX_NAME_LENGTH = 50;

    /**
     * 카드에 표시할 수 있는 개수를 한참 넘는 태그는 실수이거나 남용이다. 초과분을 버리면
     * 어떤 게 빠졌는지 알 수 없으므로 400으로 막는다.
     */
    private static final int MAX_TAGS_PER_DIARY = 10;

    /**
     * 검색은 입력 중에 계속 호출되는 자동완성용이라 한 화면에 들어갈 만큼만 내려준다.
     */
    private static final int SEARCH_LIMIT = 20;

    /**
     * TagRepository.search의 escape 절과 반드시 같은 문자여야 한다.
     */
    private static final char LIKE_ESCAPE = '!';

    private final TagRepository tagRepository;
    private final DiaryTagRepository diaryTagRepository;

    /**
     * 부분 일치·대소문자 무시 검색. 검색어가 없거나 공백뿐이면 <b>빈 배열</b>이다 — 여기서
     * 전체 태그를 내려주면 자동완성 입력창이 비어 있을 때 사전을 통째로 뿌리게 된다.
     */
    @Transactional(readOnly = true)
    public List<TagResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return tagRepository.search(likePattern(keyword.trim()), PageRequest.ofSize(SEARCH_LIMIT)).stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                .toList();
    }

    /**
     * 일기의 태그를 넘어온 목록으로 <b>교체</b>한다(부분 추가가 아니다). 빈 목록이면 전부 떼어낸다.
     *
     * <p>기존 연결을 지우고 다시 거는 방식이라 "무엇이 추가·삭제됐는지"를 계산할 필요가 없다.
     * 일기당 태그가 최대 10개뿐이라, 차이를 구해서 얻는 이득보다 규칙이 단순한 쪽이 낫다.
     * 전역 tags 행은 지우지 않는다 — 다른 일기가 같은 태그를 쓰고 있을 수 있고, 아무도 쓰지
     * 않는 태그가 남아도 검색에만 뜰 뿐 문제가 없다.
     */
    @Transactional
    public List<TagResponse> replaceTags(Diary diary, List<String> names) {
        List<String> normalized = normalize(names);

        diaryTagRepository.deleteByDiaryId(diary.getId());
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = upsert(normalized);
        diaryTagRepository.saveAll(tags.stream()
                .map(tag -> DiaryTag.builder().diary(diary).tag(tag).build())
                .toList());

        // 방금 만든 목록을 그대로 돌려주지 않고 다시 읽는다. 여기서 반환하면 순서가 "사용자가
        // 입력한 순"이 되는데 조회 경로는 이름순이라, 같은 일기의 태그가 수정 응답과 상세
        // 조회에서 다른 순서로 보인다. 쓰기는 드물어 쿼리 한 번을 더 쓰는 편이 낫다.
        return findTags(diary.getId());
    }

    /**
     * 일기 한 건의 태그. 상세 조회처럼 건수가 하나일 때만 쓴다.
     */
    @Transactional(readOnly = true)
    public List<TagResponse> findTags(Long diaryId) {
        return findTagsByDiaryIds(List.of(diaryId)).getOrDefault(diaryId, List.of());
    }

    /**
     * 목록 경로(피드·탐색·랜덤 추천)가 쓰는 진입점. 건수와 무관하게 쿼리 한 번이다.
     *
     * @return 태그가 하나도 없는 일기는 키 자체가 없다. 호출자가 빈 목록으로 채운다.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<TagResponse>> findTagsByDiaryIds(List<Long> diaryIds) {
        if (diaryIds.isEmpty()) {
            return Map.of();
        }

        return diaryTagRepository.findTagRowsByDiaryIds(diaryIds).stream()
                .collect(Collectors.groupingBy(
                        DiaryTagRepository.DiaryTagRow::getDiaryId,
                        Collectors.mapping(
                                row -> new TagResponse(row.getTagId(), row.getName()),
                                Collectors.toList())));
    }

    /**
     * 앞뒤 공백을 떼고, 빈 이름은 버리고, 대소문자를 무시해 중복을 없앤다.
     *
     * <p>빈 이름만 조용히 버리는 이유: 프론트가 입력창을 쉼표로 쪼개면 마지막에 빈 칸이
     * 딸려오기 쉬운데, 그것 때문에 발행이 400으로 막히면 사용자가 원인을 알 수 없다.
     * 반면 길이·개수 초과는 사용자가 실제로 입력한 내용이 사라지는 것이라 에러로 알린다.
     *
     * @return 입력 순서를 유지한 목록. 저장되는 철자는 처음 나온 것을 따른다.
     */
    private List<String> normalize(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        Map<String, String> unique = new LinkedHashMap<>();
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.length() > MAX_NAME_LENGTH) {
                throw new InvalidTagNameException(MAX_NAME_LENGTH);
            }
            unique.putIfAbsent(lower(trimmed), trimmed);
        }

        if (unique.size() > MAX_TAGS_PER_DIARY) {
            throw new TooManyTagsException(MAX_TAGS_PER_DIARY);
        }

        return List.copyOf(unique.values());
    }

    /**
     * 이미 있는 태그는 그대로 쓰고 없는 것만 만든다. 흔한 경우(전부 기존 태그)에는 조회
     * 한 번으로 끝나고, 새 태그가 있을 때만 그 개수만큼 insert와 재조회가 붙는다.
     */
    private List<Tag> upsert(List<String> names) {
        List<String> lowerNames = names.stream().map(this::lower).toList();
        Map<String, Tag> found = byLowerName(tagRepository.findByLowerNameIn(lowerNames));

        List<String> missing = names.stream()
                .filter(name -> !found.containsKey(lower(name)))
                .toList();
        if (missing.isEmpty()) {
            return names.stream().map(name -> found.get(lower(name))).toList();
        }

        missing.forEach(tagRepository::insertIfAbsent);

        // insert가 네이티브라 영속성 컨텍스트에 새 Tag가 없다. id를 얻으려면 다시 읽어야 한다.
        Map<String, Tag> all = byLowerName(tagRepository.findByLowerNameIn(lowerNames));
        return names.stream().map(name -> all.get(lower(name))).toList();
    }

    private Map<String, Tag> byLowerName(List<Tag> tags) {
        return tags.stream().collect(Collectors.toMap(
                tag -> lower(tag.getName()), Function.identity(), (first, next) -> first));
    }

    /**
     * LIKE 와일드카드(% _)와 escape 문자 자체를 검색어에서 무력화한다. 이스케이프하지 않으면
     * "50%" 같은 검색어가 전체 태그를 긁어온다.
     *
     * <p>escape 문자가 흔한 백슬래시가 아닌 이유는 TagRepository.search의 주석 참고.
     */
    private String likePattern(String keyword) {
        StringBuilder escaped = new StringBuilder("%");
        for (char c : keyword.toCharArray()) {
            if (c == LIKE_ESCAPE || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.append('%').toString();
    }

    /**
     * 터키어 로케일에서 'I'가 점 없는 소문자로 바뀌는 것 같은 로케일 의존을 피한다.
     * DB의 lower()와 판정이 갈리면 같은 태그가 두 행으로 생긴다.
     */
    private String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
