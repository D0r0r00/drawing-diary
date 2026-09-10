-- 프론트가 모든 화면을 실제로 눌러볼 수 있도록 전체 시나리오 데이터를 채운다.
-- 계정이 하나뿐인 DB에서는 팔로잉 피드, 팔로워 목록, 랭킹, 알림, "이어서 그리기"가
-- 전부 빈 화면이라 화면 자체를 검증할 수 없다. 이 스크립트는 그 빈칸을 메운다.
--
-- 실행:
--   psql "<연결 문자열>" -f infra/seed-full-scenario.sql
--
-- 성질:
--   * 멱등하다. 몇 번을 실행해도 같은 상태로 수렴한다(중복 행이 생기지 않는다).
--   * 기존 행을 지우거나 덮어쓰지 않는다. 예외는 아래 셋뿐이다:
--     - 시드 계정의 profile_img_url / bio 가 비었거나 example.com 가짜 URL일 때만 채운다
--     - 시드 계정이 쓴 기존 일기의 category_id 가 NULL 일 때만 채운다
--     - 시드 계정 10개의 비밀번호는 password123 으로 맞춘다(아래 1번 블록 주석 참고)
--   * user_id 를 하드코딩하지 않는다. 사람은 전부 이메일로 찾는다.
--     로컬 DB와 Railway DB의 id 체계가 서로 다르기 때문이다.
--
-- 등장인물(A~D 4명 + 구경꾼 6명):
--   A test3@example.com        기존 계정. 비밀번호는 건드리지 않는다(없으면 새로 만든다).
--   B seed.yuna@example.com  C seed.dohyun@example.com  D seed.sora@example.com
--   구경꾼 seed.reader1~6@example.com — 좋아요 수를 0~8로 벌리려고 두는 조연.
--   새로 만드는 계정의 비밀번호는 전부 password123.

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. 계정
--
-- 이메일로 이미 있으면 새로 만들지 않고, 비어 있는 프로필만 채운다. 닉네임에는 unique
-- 제약이 있어서 다른 사람이 이미 쓰고 있으면 뒤에 숫자를 붙여 비켜간다 — 시드가
-- 남의 데이터 때문에 통째로 실패하는 일을 막으려는 것이다.
--
-- 비밀번호는 아래 10개 계정에 한해 password123 으로 맞춘다. 프론트가 계정마다 다른
-- 비밀번호를 들고 다니지 않아도 되게 하려는 것으로, 특히 이 스크립트보다 먼저 있던
-- test3@example.com 이 대상이다. 전부 example.com 테스트 계정이고 목록이 여기 고정돼
-- 있어서 다른 사용자에게는 닿지 않는다.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    -- BCryptPasswordEncoder(강도 10)로 만든 "password123"의 해시.
    pw CONSTANT text := '$2a$10$YPD9ifcScvSrtXOWlR6TLOJ3cnG7FUAbG1KcdZ5pnXhvFlPAMqg6S';
    r        record;
    nick     text;
    n        int;
    made     int := 0;
    patched  int := 0;
    repw     int := 0;
BEGIN
    FOR r IN
        SELECT * FROM (VALUES
            ('test3@example.com',        '민서',    '그림일기 3년차. 하루에 한 컷씩 남기는 중.',   'dd-user-a', 120),
            ('seed.yuna@example.com',    '유나',    '카페랑 창밖 고양이를 주로 그려요. 색연필파.', 'dd-user-b',  90),
            ('seed.dohyun@example.com',  '도현',    '자전거 타고 다니면서 본 풍경을 그립니다.',    'dd-user-c',  75),
            ('seed.sora@example.com',    '소라',    '먹은 것만 그리는 계정. 대체로 야식.',         'dd-user-d',  60),
            ('seed.reader1@example.com', '구경꾼1', '조용히 구경만 합니다.',                       'dd-reader1', 50),
            ('seed.reader2@example.com', '구경꾼2', '좋아요 누르는 게 취미.',                      'dd-reader2', 45),
            ('seed.reader3@example.com', '구경꾼3', '언젠가 저도 그려볼게요.',                     'dd-reader3', 40),
            ('seed.reader4@example.com', '구경꾼4', '그림 잘 그리는 사람 구경하는 계정.',          'dd-reader4', 35),
            ('seed.reader5@example.com', '구경꾼5', '야경 그림만 보면 멈춥니다.',                  'dd-reader5', 30),
            ('seed.reader6@example.com', '구경꾼6', '맛있는 그림 찾아다니는 중.',                  'dd-reader6', 25)
        ) AS t(email, nickname, bio, avatar, days_ago)
    LOOP
        IF EXISTS (SELECT 1 FROM users WHERE email = r.email) THEN
            -- 이미 있는 계정: 비어 있는 프로필만 채운다. example.com 은 실제로 뜨지 않는
            -- 가짜 URL이라 "비어 있음"과 같게 취급한다.
            UPDATE users
               SET profile_img_url = CASE
                       WHEN profile_img_url IS NULL OR profile_img_url = ''
                            OR profile_img_url LIKE 'https://example.com/%'
                       THEN 'https://picsum.photos/seed/' || r.avatar || '/200/200'
                       ELSE profile_img_url END,
                   bio = CASE
                       WHEN bio IS NULL OR bio = '' THEN r.bio
                       ELSE bio END
             WHERE email = r.email
               AND (profile_img_url IS NULL OR profile_img_url = ''
                    OR profile_img_url LIKE 'https://example.com/%'
                    OR bio IS NULL OR bio = '');
            IF FOUND THEN
                patched := patched + 1;
            END IF;

            -- 비밀번호 통일. bcrypt 는 salt 가 섞여 있어 SQL 로는 "이 해시가 password123 인가"를
            -- 검사할 수 없으므로, 항상 같은 해시 문자열을 넣고 그것과 다를 때만 쓴다.
            -- 덕분에 두 번째 실행부터는 0건이 되어 재실행이 조용하다.
            UPDATE users SET password = pw WHERE email = r.email AND password <> pw;
            IF FOUND THEN
                repw := repw + 1;
            END IF;

            CONTINUE;
        END IF;

        nick := r.nickname;
        n := 1;
        WHILE EXISTS (SELECT 1 FROM users WHERE nickname = nick) LOOP
            n := n + 1;
            nick := r.nickname || n::text;
            RAISE NOTICE '닉네임 %가 이미 쓰이고 있어 %로 바꿉니다.', r.nickname, nick;
        END LOOP;

        INSERT INTO users (email, password, nickname, profile_img_url, bio, created_at, updated_at)
        VALUES (r.email, pw, nick,
                'https://picsum.photos/seed/' || r.avatar || '/200/200',
                r.bio,
                now() - (r.days_ago || ' days')::interval,
                now() - (r.days_ago || ' days')::interval);
        made := made + 1;
    END LOOP;

    RAISE NOTICE '[1/10] 계정: 신규 %명, 프로필 보강 %명, 비밀번호 통일 %명', made, patched, repw;
END $seed$;

-- ---------------------------------------------------------------------------
-- 2. 카테고리 — 계정마다 3~4개
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO categories (user_id, name, created_at, updated_at)
    SELECT u.user_id, c.name, u.created_at, u.created_at
      FROM (VALUES
            ('test3@example.com',       '일상'), ('test3@example.com',       '여행'), ('test3@example.com', '음식'),
            ('seed.yuna@example.com',   '일상'), ('seed.yuna@example.com',   '카페'),
            ('seed.yuna@example.com',   '반려동물'), ('seed.yuna@example.com', '운동'),
            ('seed.dohyun@example.com', '일상'), ('seed.dohyun@example.com', '여행'), ('seed.dohyun@example.com', '취미'),
            ('seed.sora@example.com',   '일상'), ('seed.sora@example.com',   '음식'), ('seed.sora@example.com', '기록')
           ) AS c(email, name)
      JOIN users u ON u.email = c.email AND u.deleted_at IS NULL
    ON CONFLICT (user_id, name) DO NOTHING;

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[2/10] 카테고리: %개 추가', made;
END $seed$;

-- ---------------------------------------------------------------------------
-- 3. 태그 사전
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO tags (name)
    SELECT * FROM (VALUES
        ('일상'), ('오운완'), ('여행'), ('맛집'), ('카페'),
        ('산책'), ('비오는날'), ('고양이'), ('야경'), ('드로잉')
    ) AS t(name)
    ON CONFLICT (name) DO NOTHING;

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[3/10] 태그: %개 추가', made;
END $seed$;

-- ---------------------------------------------------------------------------
-- 4. 팔로우
--
-- 맞팔(A<->B, A<->C)과 단방향(B->C, C->D, D->A, D->B)을 섞는다. 이래야 프론트가
-- "팔로우" 버튼과 "팔로잉 중" 버튼을 같은 화면에서 둘 다 볼 수 있다.
-- 구경꾼들은 A~D를 따라가기만 해서 팔로워 목록에 사람이 여러 명 쌓이게 한다.
-- 구경꾼1은 아무도 자기를 팔로우하지 않아, 팔로워 0명인 프로필도 확인할 수 있다.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO follows (follower_id, following_id)
    SELECT f1.user_id, f2.user_id
      FROM (VALUES
            -- 맞팔 2쌍
            ('test3@example.com',        'seed.yuna@example.com'),
            ('seed.yuna@example.com',    'test3@example.com'),
            ('test3@example.com',        'seed.dohyun@example.com'),
            ('seed.dohyun@example.com',  'test3@example.com'),
            -- 단방향
            ('seed.yuna@example.com',    'seed.dohyun@example.com'),
            ('seed.dohyun@example.com',  'seed.sora@example.com'),
            ('seed.sora@example.com',    'test3@example.com'),
            ('seed.sora@example.com',    'seed.yuna@example.com'),
            -- 구경꾼
            ('seed.reader1@example.com', 'test3@example.com'),
            ('seed.reader2@example.com', 'test3@example.com'),
            ('seed.reader6@example.com', 'test3@example.com'),
            ('seed.reader1@example.com', 'seed.yuna@example.com'),
            ('seed.reader4@example.com', 'seed.yuna@example.com'),
            ('seed.reader2@example.com', 'seed.dohyun@example.com'),
            ('seed.reader5@example.com', 'seed.dohyun@example.com'),
            ('seed.reader3@example.com', 'seed.sora@example.com'),
            ('seed.reader6@example.com', 'seed.sora@example.com')
           ) AS f(follower, following)
      JOIN users f1 ON f1.email = f.follower  AND f1.deleted_at IS NULL
      JOIN users f2 ON f2.email = f.following AND f2.deleted_at IS NULL
    ON CONFLICT (follower_id, following_id) DO NOTHING;

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[4/10] 팔로우: %건 추가', made;
END $seed$;

-- ---------------------------------------------------------------------------
-- 5. 일기 15건 (+ 방, 방 멤버, 협업자, 카테고리)
--
-- RoomService.submit()이 하는 일을 그대로 따라간다: 방을 FINISHED로 만들고 그 방의
-- 멤버를 협업자로 복사한다. diary_collaborators가 비면 피드가 작성자를 찾지 못한다
-- (DiaryService.findAuthors). 작성자 = 협업자 중 가장 먼저 등록된 사람이라
-- 방장을 반드시 먼저 넣는다.
--
-- 멱등 키는 final_img_url 이다. 제목은 남과 겹칠 수 있지만 picsum 시드 슬러그는
-- 이 스크립트만 쓰는 값이라 이 일기가 내가 만든 것인지 정확히 가려낸다.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    d      record;
    owner  bigint;
    mate   bigint;
    cat    bigint;
    room   bigint;
    diary  bigint;
    img    text;
    made   int := 0;
BEGIN
    FOR d IN
        SELECT * FROM (VALUES
            ('dd-s-01', '봄볕 드는 창가', 'seed.yuna@example.com', NULL,
             '오후 두 시쯤 창가에 빛이 길게 들어왔다. 커튼 그림자가 바닥에 줄무늬를 만드는 걸 그대로 옮겨봤다. 노란색을 너무 많이 써서 다시 덜어냈다.',
             'PUBLIC', '일상', 2),
            ('dd-s-02', '라떼 아트 첫 성공', 'seed.yuna@example.com', 'seed.dohyun@example.com',
             '열 번쯤 망하고 나서야 하트 비슷한 게 나왔다. 사진부터 찍고 마시느라 다 식었지만 기분은 좋았다.',
             'PUBLIC', '카페', 5),
            ('dd-s-03', '창밖 고양이 관찰일지', 'seed.yuna@example.com', NULL,
             '옆집 고양이가 담벼락에 앉아 한 시간을 안 움직였다. 꼬리 끝만 가끔 튕기듯 움직이는데 그게 제일 그리기 어려웠다.',
             'FOLLOWERS_ONLY', '반려동물', 9),
            ('dd-s-04', '오늘도 스쿼트 100개', 'seed.yuna@example.com', NULL,
             '운동 기록용 그림. 다리 그리기가 귀찮아서 점점 단순해지고 있다. 그래도 3주째 이어가는 중.',
             'PUBLIC', '운동', 14),
            ('dd-s-05', '아무한테도 안 보여줄 낙서', 'seed.yuna@example.com', NULL,
             '망한 그림도 남겨두려고 비공개로 저장. 나중에 보면 이것도 기록이 될 것 같아서.',
             'PRIVATE', NULL, 20),
            ('dd-s-06', '한강 야경 자전거', 'seed.dohyun@example.com', 'seed.yuna@example.com',
             '밤 열 시에 한강을 달렸다. 반대편 건물 불빛이 물에 번지는 게 예뻐서 멈춰 서서 한참 봤다. 검정 위에 색을 얹는 방식으로 그렸다.',
             'PUBLIC', '취미', 1),
            ('dd-s-07', '강릉 바다 스케치', 'seed.dohyun@example.com', NULL,
             '기차 타고 두 시간. 도착하자마자 바다부터 봤다. 파도 선을 몇 번이나 지웠는지 모르겠다.',
             'PUBLIC', '여행', 7),
            ('dd-s-08', '퇴근길 골목', 'seed.dohyun@example.com', NULL,
             '집 가는 길에 늘 지나는 골목인데 오늘따라 조명이 달라 보였다. 팔로워한테만 보여주는 소소한 기록.',
             'FOLLOWERS_ONLY', NULL, 12),
            ('dd-s-09', '야식 떡볶이 기록', 'seed.sora@example.com', NULL,
             '밤 열한 시에 떡볶이를 시켰다. 국물 색을 내려다 색을 너무 겹쳐 써서 탁해졌다. 다음엔 얇게 여러 번 칠해봐야지.',
             'PUBLIC', '음식', 3),
            ('dd-s-10', '셋이 같이 그린 여름', 'seed.sora@example.com', 'test3@example.com,seed.yuna@example.com',
             '한 명은 하늘, 한 명은 나무, 한 명은 사람을 맡았다. 스타일이 다 다른데 이상하게 잘 어울렸다.',
             'PUBLIC', '기록', 4),
            ('dd-s-11', '혼자 먹은 생일 케이크', 'seed.sora@example.com', NULL,
             '조각 케이크 하나에 초 하나 꽂았다. 비공개로 남겨두는 기록.',
             'PRIVATE', '일상', 17),
            ('dd-s-12', '새벽 네 시의 편의점', 'test3@example.com', NULL,
             '잠이 안 와서 나갔다가 편의점 불빛만 그리고 왔다. 형광등 흰색을 종이에 어떻게 내야 할지 아직 모르겠다.',
             'PUBLIC', '일상', 6),
            ('dd-s-13', '제주 돌담길', 'test3@example.com', 'seed.dohyun@example.com',
             '돌 하나하나 다르게 그리려다 손목이 아팠다. 결국 절반은 대충 넘겼는데 멀리서 보니 티가 안 난다.',
             'PUBLIC', '여행', 10),
            ('dd-s-14', '비 오는 날의 국물', 'test3@example.com', NULL,
             '비 오는 날엔 뜨거운 국물. 김이 올라오는 걸 그리려고 흰색을 남겨뒀는데 생각보다 잘 나왔다.',
             'FOLLOWERS_ONLY', '음식', 15),
            ('dd-s-15', '아직 정리 못 한 일기', 'test3@example.com', NULL,
             '쓰다 만 글이랑 밑그림만 있는 상태로 저장해뒀다. 공개할 생각은 아직 없다.',
             'PRIVATE', NULL, 25)
        ) AS t(slug, title, owner_email, mate_emails, content, visibility, category, days_ago)
    LOOP
        img := 'https://picsum.photos/seed/' || d.slug || '/800/600';

        IF EXISTS (SELECT 1 FROM diaries WHERE final_img_url = img) THEN
            CONTINUE;
        END IF;

        SELECT user_id INTO owner FROM users WHERE email = d.owner_email AND deleted_at IS NULL;
        IF owner IS NULL THEN
            RAISE EXCEPTION '작성자 %를 찾을 수 없습니다.', d.owner_email;
        END IF;

        cat := NULL;
        IF d.category IS NOT NULL THEN
            SELECT category_id INTO cat FROM categories WHERE user_id = owner AND name = d.category;
        END IF;

        INSERT INTO drawing_rooms (owner_id, status, created_at, updated_at)
        VALUES (owner, 'FINISHED',
                now() - (d.days_ago || ' days')::interval,
                now() - (d.days_ago || ' days')::interval)
        RETURNING room_id INTO room;

        -- 방장이 먼저. room_members 등록 순서가 곧 협업자 순서이고,
        -- 협업자 중 첫 번째가 작성자로 표시된다.
        INSERT INTO room_members (room_id, user_id, joined_at)
        VALUES (room, owner, now() - (d.days_ago || ' days')::interval);

        IF d.mate_emails IS NOT NULL THEN
            FOR mate IN
                SELECT u.user_id
                  FROM unnest(string_to_array(d.mate_emails, ',')) WITH ORDINALITY AS e(email, ord)
                  JOIN users u ON u.email = e.email AND u.deleted_at IS NULL
                 ORDER BY e.ord
            LOOP
                INSERT INTO room_members (room_id, user_id, joined_at)
                VALUES (room, mate, now() - (d.days_ago || ' days')::interval + interval '10 minutes')
                ON CONFLICT (room_id, user_id) DO NOTHING;
            END LOOP;
        END IF;

        INSERT INTO diaries (room_id, category_id, theme_id, title, content,
                             final_img_url, visibility, created_at, updated_at)
        VALUES (room, cat, NULL, d.title, d.content, img, d.visibility,
                now() - (d.days_ago || ' days')::interval,
                now() - (d.days_ago || ' days')::interval)
        RETURNING diary_id INTO diary;

        INSERT INTO diary_collaborators (diary_id, user_id)
        SELECT diary, rm.user_id FROM room_members rm WHERE rm.room_id = room ORDER BY rm.id;

        made := made + 1;
    END LOOP;

    RAISE NOTICE '[5/10] 일기: %건 추가', made;
END $seed$;

-- 5-b. 일기 태그 — 1~3개씩. dd-s-05 / 11 / 15 는 일부러 태그 없이 둔다.
DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO diary_tags (diary_id, tag_id)
    SELECT d.diary_id, t.tag_id
      FROM (VALUES
            ('dd-s-01', '일상'), ('dd-s-01', '드로잉'),
            ('dd-s-02', '카페'), ('dd-s-02', '일상'), ('dd-s-02', '드로잉'),
            ('dd-s-03', '고양이'), ('dd-s-03', '일상'),
            ('dd-s-04', '오운완'),
            ('dd-s-06', '야경'), ('dd-s-06', '산책'), ('dd-s-06', '드로잉'),
            ('dd-s-07', '여행'),
            ('dd-s-08', '산책'), ('dd-s-08', '일상'),
            ('dd-s-09', '맛집'), ('dd-s-09', '일상'),
            ('dd-s-10', '일상'), ('dd-s-10', '드로잉'),
            ('dd-s-12', '야경'), ('dd-s-12', '산책'),
            ('dd-s-13', '여행'), ('dd-s-13', '드로잉'),
            ('dd-s-14', '비오는날'), ('dd-s-14', '맛집')
           ) AS x(slug, tag)
      JOIN diaries d ON d.final_img_url = 'https://picsum.photos/seed/' || x.slug || '/800/600'
      JOIN tags t ON t.name = x.tag
     WHERE NOT EXISTS (
            SELECT 1 FROM diary_tags dt WHERE dt.diary_id = d.diary_id AND dt.tag_id = t.tag_id);

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[5b/10] 일기-태그 연결: %건 추가', made;
END $seed$;

-- ---------------------------------------------------------------------------
-- 6. 좋아요 / 댓글
--
-- 좋아요와 댓글을 남긴 사람은 전부 그 일기를 실제로 볼 수 있는 사람이다(FOLLOWERS_ONLY면
-- 작성자를 팔로우 중, PRIVATE면 아무도 없음). 서버 규칙과 어긋나는 데이터를 넣으면
-- 프론트가 "좋아요는 달렸는데 열면 403" 같은 상태를 보게 된다.
-- created_at 은 일기 작성 시각 이후로 흩어놓아 목록 정렬이 자연스럽게 보이게 한다.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO likes (diary_id, user_id, created_at)
    SELECT d.diary_id, u.user_id, d.created_at + (x.ord * interval '2 hours')
      FROM (VALUES
            ('dd-s-01', 'test3@example.com,seed.dohyun@example.com,seed.sora@example.com,seed.reader1@example.com,seed.reader2@example.com,seed.reader3@example.com,seed.reader4@example.com,seed.reader5@example.com'),
            ('dd-s-02', 'test3@example.com,seed.sora@example.com,seed.reader1@example.com,seed.reader2@example.com,seed.reader6@example.com'),
            ('dd-s-03', 'test3@example.com,seed.sora@example.com'),
            ('dd-s-04', 'seed.reader1@example.com,seed.reader4@example.com'),
            ('dd-s-06', 'test3@example.com,seed.sora@example.com,seed.reader1@example.com,seed.reader2@example.com,seed.reader5@example.com,seed.reader6@example.com'),
            ('dd-s-07', 'test3@example.com,seed.reader2@example.com,seed.reader5@example.com'),
            ('dd-s-08', 'test3@example.com,seed.yuna@example.com'),
            ('dd-s-09', 'test3@example.com,seed.yuna@example.com,seed.dohyun@example.com,seed.reader3@example.com,seed.reader6@example.com'),
            ('dd-s-10', 'seed.dohyun@example.com,seed.reader1@example.com,seed.reader2@example.com,seed.reader3@example.com,seed.reader4@example.com,seed.reader5@example.com,seed.reader6@example.com'),
            ('dd-s-12', 'seed.yuna@example.com,seed.dohyun@example.com,seed.reader1@example.com,seed.reader5@example.com'),
            ('dd-s-13', 'seed.yuna@example.com,seed.sora@example.com,seed.reader2@example.com,seed.reader3@example.com,seed.reader4@example.com,seed.reader6@example.com'),
            ('dd-s-14', 'seed.yuna@example.com,seed.dohyun@example.com,seed.sora@example.com')
            -- dd-s-05 / 11 / 15 는 PRIVATE 이라 좋아요 0건.
           ) AS l(slug, emails)
      CROSS JOIN LATERAL unnest(string_to_array(l.emails, ',')) WITH ORDINALITY AS x(email, ord)
      JOIN diaries d ON d.final_img_url = 'https://picsum.photos/seed/' || l.slug || '/800/600'
      JOIN users u ON u.email = x.email AND u.deleted_at IS NULL
    ON CONFLICT (diary_id, user_id) DO NOTHING;

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[6/10] 좋아요: %건 추가', made;
END $seed$;

DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO comments (diary_id, user_id, content, created_at, updated_at)
    SELECT d.diary_id, u.user_id, c.content,
           d.created_at + (c.ord * interval '5 hours'),
           d.created_at + (c.ord * interval '5 hours')
      FROM (VALUES
            ('dd-s-01', 'test3@example.com',        '창가 빛 표현이 진짜 좋네요. 이 시간대 색이 딱 이래요.', 1),
            ('dd-s-01', 'seed.dohyun@example.com',  '노란색 어떻게 낸 거예요? 색연필인가요?',                2),
            ('dd-s-01', 'seed.reader1@example.com', '아침에 보기 딱 좋은 그림이에요.',                       3),
            ('dd-s-01', 'seed.reader3@example.com', '저도 이런 창가 있는 집에 살고 싶다...',                 4),
            ('dd-s-02', 'test3@example.com',        '열 번 만에 성공이면 훌륭한데요. 축하해요.',             1),
            ('dd-s-02', 'seed.reader2@example.com', '하트 모양 귀엽게 잘 나왔네요ㅋㅋ',                      2),
            ('dd-s-03', 'test3@example.com',        '고양이 표정이 살아있어요. 꼬리 선이 특히 좋네요.',      1),
            ('dd-s-06', 'test3@example.com',        '검정 위에 색 얹은 거 효과 확실하네요.',                 1),
            ('dd-s-06', 'seed.yuna@example.com',    '같이 그려서 즐거웠어요. 다음에 또 해요.',               2),
            ('dd-s-06', 'seed.reader5@example.com', '한강 자주 가는데 딱 이 느낌이에요.',                    3),
            ('dd-s-06', 'seed.sora@example.com',    '밤에 자전거 타는 거 부럽다...',                         4),
            ('dd-s-06', 'seed.reader1@example.com', '구도가 시원해서 좋아요.',                               5),
            ('dd-s-07', 'seed.reader2@example.com', '강릉 또 가고 싶어지는 그림이네요.',                     1),
            ('dd-s-08', 'test3@example.com',        '이 골목 어디예요? 분위기 좋다.',                        1),
            ('dd-s-08', 'seed.yuna@example.com',    '퇴근길에 이런 거 보이면 하루가 좀 풀리죠.',             2),
            ('dd-s-09', 'seed.yuna@example.com',    '떡볶이는 진리입니다.',                                  1),
            ('dd-s-09', 'seed.dohyun@example.com',  '야식 그림은 반칙이에요. 배고파요.',                     2),
            ('dd-s-09', 'seed.reader6@example.com', '국물색 탁해진 게 오히려 진해 보여요.',                  3),
            ('dd-s-10', 'test3@example.com',        '셋이 그리니까 확실히 다르네요. 재밌었어요.',            1),
            ('dd-s-10', 'seed.reader4@example.com', '여름 색이 그대로 담겼어요.',                            2),
            ('dd-s-12', 'seed.yuna@example.com',    '형광등 흰색 그거 어렵죠. 저도 매번 실패해요.',          1),
            ('dd-s-12', 'seed.reader5@example.com', '새벽 감성 제대로네요ㅠㅠ',                               2),
            ('dd-s-13', 'seed.dohyun@example.com',  '돌담 질감 살린 거 대단합니다.',                          1),
            ('dd-s-13', 'seed.sora@example.com',    '제주 가고 싶어졌어요.',                                 2),
            ('dd-s-13', 'seed.reader3@example.com', '다음엔 저도 껴주세요!',                                 3),
            ('dd-s-14', 'seed.yuna@example.com',    '비 오는 날엔 역시 국물이죠.',                            1),
            ('dd-s-14', 'seed.sora@example.com',    '이 그림 보고 라면 끓였습니다.',                          2)
            -- dd-s-04 / 05 / 11 / 15 는 댓글 0건.
           ) AS c(slug, email, content, ord)
      JOIN diaries d ON d.final_img_url = 'https://picsum.photos/seed/' || c.slug || '/800/600'
      JOIN users u ON u.email = c.email AND u.deleted_at IS NULL
     WHERE NOT EXISTS (
            SELECT 1 FROM comments cm
             WHERE cm.diary_id = d.diary_id AND cm.user_id = u.user_id AND cm.content = c.content);

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[6b/10] 댓글: %건 추가', made;
END $seed$;

-- ---------------------------------------------------------------------------
-- 7. AI 점수 — PUBLIC 일기 대부분에
--
-- total_score 는 상수로 박지 않고 ScoreCalculator 와 같은 식으로 계산한다.
--   likeScore  = min(좋아요 수 * 5, 100)
--   totalScore = round(relevance*0.5 + color*0.3 + likeScore*0.2)
-- 좋아요 수를 DB에서 직접 세므로 위 6번 블록과 항상 아귀가 맞는다. Postgres 의
-- round(numeric) 은 반올림(half-up)이라 Java 의 Math.round 와 같은 값이 나온다.
--
-- theme_score 는 랭킹에서 빠졌지만 컬럼이 NOT NULL 이라 0으로 채운다(AiScore 주석 참고).
-- dd-s-04 는 점수를 일부러 넣지 않는다 — "AI 점수 없는 공개 일기" 화면 확인용.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO ai_scores (diary_id, relevance_score, color_score, theme_score, total_score, ai_comment, created_at)
    SELECT d.diary_id,
           s.relevance,
           s.color,
           0,
           round(s.relevance * 0.5 + s.color * 0.3
                 + least((SELECT count(*) FROM likes l WHERE l.diary_id = d.diary_id) * 5, 100) * 0.2)::int,
           s.comment,
           d.created_at + interval '1 hour'
      FROM (VALUES
            ('dd-s-01', 94, 90, '글에서 말한 빛과 그림자의 결이 그림에 그대로 살아 있습니다. 노란색을 덜어낸 판단이 특히 좋았고 전체 색 균형도 안정적입니다.'),
            ('dd-s-02', 76, 81, '하트 모양을 강조한 구도가 글의 성취감과 잘 맞습니다. 다만 컵 바깥 배경이 비어 있어 시선이 조금 흩어집니다.'),
            ('dd-s-06', 91, 86, '어두운 바탕에 빛을 얹는 방식이 야경이라는 소재와 정확히 맞물립니다. 물에 번지는 빛의 채도 조절이 인상적입니다.'),
            ('dd-s-07', 69, 74, '바다라는 소재는 잘 전달되지만 파도 선을 여러 번 고친 흔적이 남아 있습니다. 색은 차분하게 잘 정리되어 있습니다.'),
            ('dd-s-09', 38, 44, '음식이라는 소재는 알아볼 수 있으나 글에서 말한 야식의 분위기까지는 닿지 못했습니다. 겹쳐 칠해 탁해진 부분을 정리하면 좋겠습니다.'),
            ('dd-s-10', 84, 90, '세 사람의 그림체가 다른데도 색 온도가 통일되어 하나의 장면으로 읽힙니다. 협업 일기로서 완성도가 높습니다.'),
            ('dd-s-12', 45, 52, '새벽 편의점이라는 소재가 그림에서는 다소 흐릿하게 남았습니다. 형광등 흰색을 남겨두는 방식으로 대비를 더 주면 좋아질 것입니다.'),
            ('dd-s-13', 79, 83, '돌 하나하나의 질감 차이가 글의 고생스러움을 잘 드러냅니다. 멀리서 볼 때의 덩어리감도 잘 잡혀 있습니다.')
           ) AS s(slug, relevance, color, comment)
      JOIN diaries d ON d.final_img_url = 'https://picsum.photos/seed/' || s.slug || '/800/600'
    ON CONFLICT (diary_id) DO NOTHING;

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[7/10] AI 점수: %건 추가', made;
END $seed$;

-- ---------------------------------------------------------------------------
-- 8. 작업 중인 방 (status=WAITING + canvas_data/title/content)
--
-- "이어서 그리기" 화면은 GET /api/rooms/{roomId}/canvas 가 세 필드를 다 채워
-- 내려줄 때만 검증할 수 있다. canvas_data 는 BYTEA 이고 API 는 Base64 로 감싸
-- 내려주므로, 프론트가 디코딩해서 바로 쓸 수 있는 JSON 을 바이트로 넣는다.
-- 초대(room_invites)도 같이 남겨 초대 목록과 ROOM_INVITE 알림이 비지 않게 한다.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    w        record;
    owner    bigint;
    mate     bigint;
    invitee  bigint;
    room     bigint;
    made     int := 0;
BEGIN
    FOR w IN
        SELECT * FROM (VALUES
            ('같이 그리는 가을 산책', 'seed.yuna@example.com', 'seed.dohyun@example.com', 'seed.sora@example.com',
             '아직 밑그림만 그렸다. 나뭇잎 색은 내일 같이 채우기로 했다.',
             '{"version":1,"width":1080,"height":1080,"strokes":[{"tool":"pencil","color":"#3b3b3b","size":3,"points":[[120,540],[180,520],[240,530],[300,505]]},{"tool":"pencil","color":"#3b3b3b","size":3,"points":[[300,505],[360,540],[420,560]]},{"tool":"brush","color":"#c9a227","size":18,"points":[[640,300],[700,340],[760,320]]}]}',
             2),
            ('미완성 야식 지도', 'test3@example.com', 'seed.sora@example.com', 'seed.dohyun@example.com',
             '떡볶이랑 치킨까지는 그렸는데 배경이 통째로 비어 있다.',
             '{"version":1,"width":1080,"height":1080,"strokes":[{"tool":"brush","color":"#d94f30","size":24,"points":[[300,400],[340,430],[380,410],[420,445]]},{"tool":"pencil","color":"#5a3b1a","size":4,"points":[[600,600],[660,620],[720,600],[780,640]]}]}',
             1)
        ) AS t(title, owner_email, mate_email, invitee_email, content, canvas, days_ago)
    LOOP
        SELECT user_id INTO owner   FROM users WHERE email = w.owner_email   AND deleted_at IS NULL;
        SELECT user_id INTO mate    FROM users WHERE email = w.mate_email    AND deleted_at IS NULL;
        SELECT user_id INTO invitee FROM users WHERE email = w.invitee_email AND deleted_at IS NULL;

        SELECT room_id INTO room
          FROM drawing_rooms
         WHERE owner_id = owner AND title = w.title AND status = 'WAITING' AND deleted_at IS NULL
         LIMIT 1;

        IF room IS NULL THEN
            INSERT INTO drawing_rooms (owner_id, status, title, content, canvas_data, created_at, updated_at)
            VALUES (owner, 'WAITING', w.title, w.content, convert_to(w.canvas, 'UTF8'),
                    now() - (w.days_ago || ' days')::interval,
                    now() - (w.days_ago || ' days')::interval + interval '3 hours')
            RETURNING room_id INTO room;
            made := made + 1;
        END IF;

        INSERT INTO room_members (room_id, user_id, joined_at)
        VALUES (room, owner, now() - (w.days_ago || ' days')::interval)
        ON CONFLICT (room_id, user_id) DO NOTHING;

        INSERT INTO room_members (room_id, user_id, joined_at)
        VALUES (room, mate, now() - (w.days_ago || ' days')::interval + interval '20 minutes')
        ON CONFLICT (room_id, user_id) DO NOTHING;

        -- 수락해서 들어온 사람(ACCEPT)과 아직 답 안 한 사람(PENDING)을 둘 다 남긴다.
        INSERT INTO room_invites (room_id, sender_id, receiver_id, status, created_at)
        SELECT room, owner, mate, 'ACCEPT', now() - (w.days_ago || ' days')::interval + interval '5 minutes'
         WHERE NOT EXISTS (SELECT 1 FROM room_invites ri
                            WHERE ri.room_id = room AND ri.sender_id = owner AND ri.receiver_id = mate);

        INSERT INTO room_invites (room_id, sender_id, receiver_id, status, created_at)
        SELECT room, owner, invitee, 'PENDING', now() - (w.days_ago || ' days')::interval + interval '30 minutes'
         WHERE NOT EXISTS (SELECT 1 FROM room_invites ri
                            WHERE ri.room_id = room AND ri.sender_id = owner AND ri.receiver_id = invitee);
    END LOOP;

    RAISE NOTICE '[8/10] 작업 중인 방: %개 추가', made;
END $seed$;

-- ---------------------------------------------------------------------------
-- 9. 기존 일기의 빈 카테고리 채우기
--
-- 이 스크립트 이전에 만들어진 일기(예: seed-dummy-diaries.sql 의 5건)는 category_id 가
-- 전부 NULL 이라 카테고리 화면이 비어 보인다. 시드 계정이 쓴 일기 중 NULL 인 것만
-- 그 사람의 카테고리에 돌아가며 붙인다. 값이 이미 있으면 절대 덮어쓰지 않는다.
-- 이번에 만든 15건은 공개 범위별 NULL 케이스를 일부러 남겨둔 것이라 제외한다.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    updated int;
BEGIN
    WITH owned AS (
        -- 작성자 = 협업자 중 가장 먼저 등록된 사람(DiaryService.findAuthors 와 같은 규칙)
        SELECT d.diary_id,
               (SELECT dc.user_id FROM diary_collaborators dc
                 WHERE dc.diary_id = d.diary_id ORDER BY dc.id LIMIT 1) AS owner_id
          FROM diaries d
         WHERE d.category_id IS NULL
           AND (d.final_img_url IS NULL
                OR d.final_img_url NOT LIKE 'https://picsum.photos/seed/dd-s-%')
    ),
    target AS (
        SELECT o.diary_id, o.owner_id,
               row_number() OVER (PARTITION BY o.owner_id ORDER BY o.diary_id) - 1 AS seq
          FROM owned o
          JOIN users u ON u.user_id = o.owner_id AND u.deleted_at IS NULL
         WHERE u.email IN ('test3@example.com', 'seed.yuna@example.com',
                           'seed.dohyun@example.com', 'seed.sora@example.com')
    ),
    picked AS (
        SELECT t.diary_id,
               (SELECT c.category_id
                  FROM categories c
                 WHERE c.user_id = t.owner_id
                 ORDER BY c.category_id
                 OFFSET t.seq % GREATEST((SELECT count(*) FROM categories c2 WHERE c2.user_id = t.owner_id), 1)
                 LIMIT 1) AS category_id
          FROM target t
    )
    UPDATE diaries d
       SET category_id = p.category_id
      FROM picked p
     WHERE d.diary_id = p.diary_id
       AND d.category_id IS NULL
       AND p.category_id IS NOT NULL;

    GET DIAGNOSTICS updated = ROW_COUNT;
    RAISE NOTICE '[9/10] 기존 일기 카테고리 채움: %건', updated;
END $seed$;

-- ---------------------------------------------------------------------------
-- 10. 알림
--
-- 알림을 따로 지어내지 않고 위에서 만든 사건(팔로우/좋아요/댓글/방 초대)에서 그대로
-- 끌어낸다. 서비스 코드가 남기는 것과 같은 모양이어야 프론트가 실제 동작과 같은
-- 데이터를 보게 된다:
--   FOLLOW       receiver=팔로우 당한 사람, target_id=NULL     (FollowService)
--   LIKE/COMMENT receiver=일기 작성자,      target_id=diary_id (Like/CommentService)
--   ROOM_INVITE  receiver=초대받은 사람,    target_id=room_id  (RoomService)
-- 자기 자신에게 가는 알림은 NotificationService.notify 가 걸러내므로 여기서도 뺀다.
--
-- 목록은 notification_id 내림차순으로 내려가므로(NotificationRepository), 사건 시각
-- 오름차순으로 INSERT 해야 화면에서 최신순으로 보인다.
-- 읽음 여부는 "일주일 넘은 건 읽었다"로 갈라 읽은 것과 안 읽은 것을 섞는다.
-- ---------------------------------------------------------------------------
DO $seed$
DECLARE
    made int;
BEGIN
    INSERT INTO notifications (receiver_id, sender_id, type, target_id, is_read, created_at)
    SELECT ev.receiver_id, ev.sender_id, ev.type, ev.target_id,
           ev.happened_at < now() - interval '7 days',
           ev.happened_at
      FROM (
            -- 팔로우: 시각 컬럼이 없어 follow_id 순서를 시간 순서로 삼는다.
            SELECT f.following_id AS receiver_id,
                   f.follower_id  AS sender_id,
                   'FOLLOW'       AS type,
                   NULL::bigint   AS target_id,
                   now() - interval '25 days'
                       + (row_number() OVER (ORDER BY f.follow_id)) * interval '30 hours' AS happened_at
              FROM follows f
              JOIN users a ON a.user_id = f.follower_id
                          AND (a.email = 'test3@example.com' OR a.email LIKE 'seed.%@example.com')
              JOIN users b ON b.user_id = f.following_id
                          AND (b.email = 'test3@example.com' OR b.email LIKE 'seed.%@example.com')

            UNION ALL

            -- 좋아요
            SELECT (SELECT dc.user_id FROM diary_collaborators dc
                     WHERE dc.diary_id = l.diary_id ORDER BY dc.id LIMIT 1),
                   l.user_id, 'LIKE', l.diary_id, l.created_at
              FROM likes l
              JOIN diaries d ON d.diary_id = l.diary_id
             WHERE d.final_img_url LIKE 'https://picsum.photos/seed/dd-s-%'

            UNION ALL

            -- 댓글
            SELECT (SELECT dc.user_id FROM diary_collaborators dc
                     WHERE dc.diary_id = cm.diary_id ORDER BY dc.id LIMIT 1),
                   cm.user_id, 'COMMENT', cm.diary_id, cm.created_at
              FROM comments cm
              JOIN diaries d ON d.diary_id = cm.diary_id
             WHERE d.final_img_url LIKE 'https://picsum.photos/seed/dd-s-%'

            UNION ALL

            -- 방 초대
            SELECT ri.receiver_id, ri.sender_id, 'ROOM_INVITE', ri.room_id, ri.created_at
              FROM room_invites ri
              JOIN drawing_rooms dr ON dr.room_id = ri.room_id
             WHERE dr.status = 'WAITING'
               AND dr.deleted_at IS NULL
               AND dr.title IN ('같이 그리는 가을 산책', '미완성 야식 지도')
           ) AS ev(receiver_id, sender_id, type, target_id, happened_at)
     WHERE ev.receiver_id IS NOT NULL
       AND ev.receiver_id <> ev.sender_id
       AND NOT EXISTS (
            SELECT 1 FROM notifications n
             WHERE n.receiver_id = ev.receiver_id
               AND n.sender_id   IS NOT DISTINCT FROM ev.sender_id
               AND n.type        = ev.type
               AND n.target_id   IS NOT DISTINCT FROM ev.target_id)
     ORDER BY ev.happened_at;

    GET DIAGNOSTICS made = ROW_COUNT;
    RAISE NOTICE '[10/10] 알림: %건 추가', made;
END $seed$;

COMMIT;
