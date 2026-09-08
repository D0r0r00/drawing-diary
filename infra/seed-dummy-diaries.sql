-- 프론트 화면 개발용 더미 PUBLIC 일기 5건을 넣는다.
--
-- RoomService.submit()이 하는 일을 그대로 따라간다: 방을 하나 만들어 FINISHED로
-- 두고, 그 방의 멤버를 일기의 협업자로 복사한다. diary_collaborators가 채워져야
-- 피드가 작성자를 찾을 수 있으므로(DiaryService.findAuthors) 이 단계는 생략 불가.
--
-- 실행:
--   psql "<연결 문자열>" -v target_user_id=2 -f infra/seed-dummy-diaries.sql
-- target_user_id를 넘기지 않으면 2번 사용자를 대상으로 한다.
--
-- 여러 번 실행해도 안전하다: 같은 제목의 일기가 이미 그 사용자에게 있으면 건너뛴다.

\if :{?target_user_id}
\else
  \set target_user_id 2
\endif

BEGIN;

-- psql의 :변수는 $$ ... $$ 안에서는 치환되지 않으므로, 세션 설정으로 한 번 건네준다.
SELECT set_config('seed.target_user_id', :'target_user_id', false);

DO $$
DECLARE
    target_user_id CONSTANT bigint := current_setting('seed.target_user_id')::bigint;
    seed   record;
    room   bigint;
    diary  bigint;
    made   int := 0;
    skipped int := 0;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE user_id = target_user_id AND deleted_at IS NULL) THEN
        RAISE EXCEPTION 'user_id=% 인 사용자가 없거나 이미 탈퇴했습니다.', target_user_id;
    END IF;

    FOR seed IN
        SELECT * FROM (VALUES
            (1, '비 오는 날의 창가',
                '하루 종일 비가 내렸다. 창문에 맺힌 물방울이 아래로 길게 흘러내리는 걸 한참 봤다. 빗소리 때문인지 평소보다 생각이 느리게 흘러갔다.',
                'https://picsum.photos/seed/diary-rainy-window/800/600'),
            (3, '고양이와 오후 세 시',
                '낮잠 자는 고양이 옆에 앉아 그림을 그렸다. 꼬리만 가끔 움직이고 나머지는 완전히 멈춰 있었다. 그 고요함을 종이에 옮기고 싶었는데 생각보다 어려웠다.',
                'https://picsum.photos/seed/diary-cat-afternoon/800/600'),
            (6, '첫 자전거 출근',
                '오늘 처음으로 자전거를 타고 출근했다. 한강 옆 길을 달리는데 바람이 꽤 찼다. 30분 걸렸고, 도착했을 땐 이미 기분이 좋아져 있었다.',
                'https://picsum.photos/seed/diary-bike-commute/800/600'),
            (10, '주말 아침의 팬케이크',
                '늦게 일어나서 팬케이크를 구웠다. 두 장은 태웠고 세 번째부터 괜찮아졌다. 접시에 쌓아놓고 시럽을 붓는 순간이 제일 좋았다.',
                'https://picsum.photos/seed/diary-pancake/800/600'),
            (15, '밤 산책과 편의점 불빛',
                '잠이 안 와서 밤 열한 시에 동네를 한 바퀴 돌았다. 문 연 곳은 편의점뿐이었고, 그 불빛이 유난히 따뜻해 보여서 한참 서 있었다.',
                'https://picsum.photos/seed/diary-night-walk/800/600')
        ) AS t(days_ago, title, content, img)
    LOOP
        IF EXISTS (
            SELECT 1
            FROM diaries d
            JOIN diary_collaborators dc ON dc.diary_id = d.diary_id
            WHERE dc.user_id = target_user_id AND d.title = seed.title
        ) THEN
            skipped := skipped + 1;
            CONTINUE;
        END IF;

        INSERT INTO drawing_rooms (owner_id, status, created_at, updated_at)
        VALUES (target_user_id, 'FINISHED',
                now() - (seed.days_ago || ' days')::interval,
                now() - (seed.days_ago || ' days')::interval)
        RETURNING room_id INTO room;

        INSERT INTO room_members (room_id, user_id, joined_at)
        VALUES (room, target_user_id, now() - (seed.days_ago || ' days')::interval);

        INSERT INTO diaries (room_id, category_id, theme_id, title, content,
                             final_img_url, visibility, created_at, updated_at)
        VALUES (room, NULL, NULL, seed.title, seed.content,
                seed.img, 'PUBLIC',
                now() - (seed.days_ago || ' days')::interval,
                now() - (seed.days_ago || ' days')::interval)
        RETURNING diary_id INTO diary;

        -- submit()의 스냅샷과 동일: 그 시점 방 멤버 전원이 협업자가 된다.
        INSERT INTO diary_collaborators (diary_id, user_id)
        SELECT diary, rm.user_id FROM room_members rm WHERE rm.room_id = room;

        made := made + 1;
    END LOOP;

    RAISE NOTICE '완료: user_id=% 에 일기 %건 생성, %건은 이미 있어 건너뜀', target_user_id, made, skipped;
END $$;

COMMIT;

-- 결과 확인
\echo ''
\echo '── 생성된 일기 ──'
SELECT d.diary_id, d.title, d.visibility, d.final_img_url, d.created_at::date AS created
FROM diaries d
JOIN diary_collaborators dc ON dc.diary_id = d.diary_id
WHERE dc.user_id = :target_user_id
ORDER BY d.diary_id DESC
LIMIT 10;
