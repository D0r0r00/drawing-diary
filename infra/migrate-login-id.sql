-- 로그인 식별자를 email → login_id로 바꾸는 마이그레이션.
--
-- 실행:
--   드라이런(기본) — 전부 해본 뒤 ROLLBACK. 결과 표만 보고 DB는 그대로다.
--     psql "<연결 문자열>" -f infra/migrate-login-id.sql
--   실제 반영
--     psql "<연결 문자열>" -v apply=1 -f infra/migrate-login-id.sql
--
-- ⚠️ 새 백엔드 코드를 배포하기 "전에" 반영해야 한다.
--   ddl-auto: update는 컬럼을 추가만 할 뿐 UNIQUE 제거·NOT NULL 해제를 하지 않고,
--   행이 있는 테이블에 NOT NULL 컬럼을 추가하지도 못한다. 코드만 먼저 올라가면
--   가입(email 없이)과 로그인(login_id 없음)이 둘 다 실패한다.
--
-- 드라이런도 users 테이블에 잠깐(수 ms) 배타 잠금을 건다. DDL을 실제로 실행해보고
-- 되돌리는 방식이라서다.
--
-- ─── 변환 규칙 ────────────────────────────────────────────────────────────
--  아이디 규칙: ^[a-z][a-z0-9_]{3,19}$  (소문자 저장, 4~20자, 첫 글자 영문)
--
--  1. email의 @ 앞부분을 소문자로          Seed.Yuna@example.com → seed.yuna
--  2. 허용 안 되는 문자는 전부 _ 로         seed.yuna → seed_yuna
--  3. 첫 글자가 영문이 아니면 앞에 u 붙임   1234 → u1234
--  4. 20자 초과면 앞 20자만                 integ_1789061050_owner → integ_1789061050_own
--  5. 4자 미만이면 user_<user_id>           ab → user_42
--  6. 그래도 겹치면 한 명만 원래 값을 갖고, 나머지는 끝에 _<user_id>
--       우선순위: 탈퇴 안 한 계정 → user_id 작은 순
--       (20자를 넘지 않게 앞부분을 줄인다)  seed_yuna → seed_yuna_164
--
--  탈퇴한 계정도 값을 받는다(NOT NULL). 탈퇴 계정의 아이디는 새 가입에 재사용되지 않는다 —
--  앞으로 탈퇴하는 계정도 똑같이 아이디가 남으므로 과거·미래 규칙이 같다.
--
--  한 번 반영된 뒤 다시 실행하면 아무것도 바꾸지 않는다(login_id가 이미 채워진 행은 건너뜀).
-- ─────────────────────────────────────────────────────────────────────────

\set ON_ERROR_STOP on

BEGIN;

ALTER TABLE users ADD COLUMN IF NOT EXISTS login_id varchar(20);

CREATE TEMP TABLE login_id_plan ON COMMIT DROP AS
WITH base AS (
    SELECT user_id,
           email,
           deleted_at IS NOT NULL AS withdrawn,
           regexp_replace(lower(split_part(coalesce(email, ''), '@', 1)), '[^a-z0-9_]', '_', 'g') AS s
    FROM users
    WHERE login_id IS NULL
), shaped AS (
    SELECT user_id, email, withdrawn,
           CASE
               WHEN length(left(CASE WHEN s ~ '^[a-z]' THEN s ELSE 'u' || s END, 20)) < 4
                   THEN 'user_' || user_id
               ELSE left(CASE WHEN s ~ '^[a-z]' THEN s ELSE 'u' || s END, 20)
           END AS candidate
    FROM base
), ranked AS (
    SELECT s.*,
           row_number() OVER (PARTITION BY candidate ORDER BY withdrawn, user_id) AS nth,
           -- 이미 login_id를 가진 계정(재실행 시)과도 겹치면 안 된다
           EXISTS (SELECT 1 FROM users u WHERE u.login_id = s.candidate) AS taken
    FROM shaped s
)
SELECT user_id, email, withdrawn, candidate,
       CASE
           WHEN nth = 1 AND NOT taken THEN candidate
           ELSE left(candidate, 20 - length('_' || user_id)) || '_' || user_id
       END AS login_id
FROM ranked;

UPDATE users u
SET login_id = p.login_id
FROM login_id_plan p
WHERE u.user_id = p.user_id;

-- 반영 전 검증. 하나라도 걸리면 예외로 트랜잭션 전체가 취소된다.
DO $check$
DECLARE
    missing int;
    malformed int;
    dup int;
BEGIN
    SELECT count(*) INTO missing FROM users WHERE login_id IS NULL;
    SELECT count(*) INTO malformed FROM users WHERE login_id !~ '^[a-z][a-z0-9_]{3,19}$';
    SELECT count(*) INTO dup FROM (SELECT login_id FROM users GROUP BY login_id HAVING count(*) > 1) d;
    IF missing + malformed + dup > 0 THEN
        RAISE EXCEPTION 'login_id 검증 실패: 비어 있음 %, 규칙 위반 %, 중복 %', missing, malformed, dup;
    END IF;
END
$check$;

ALTER TABLE users ALTER COLUMN login_id SET NOT NULL;

-- 이름을 명시한다. 엔티티의 @UniqueConstraint(name = "uk_users_login_id")와 같아야
-- Hibernate가 기동할 때마다 이름이 다른 UNIQUE 인덱스를 하나 더 만들지 않는다.
DO $uk$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_login_id') THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_login_id UNIQUE (login_id);
    END IF;
    -- 앱을 거치지 않는 쓰기(시드 스크립트, 수동 SQL)도 규칙을 못 벗어나게 DB에서 막는다.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_users_login_id_format') THEN
        ALTER TABLE users ADD CONSTRAINT ck_users_login_id_format
            CHECK (login_id ~ '^[a-z][a-z0-9_]{3,19}$');
    END IF;
END
$uk$;

-- email의 UNIQUE 제약 제거. 이름이 Hibernate가 만든 해시(uk6dot... 등)라 환경마다
-- 같다는 보장이 없어서, 이름이 아니라 "email 컬럼 하나짜리 UNIQUE"로 찾아 지운다.
DO $email$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'users'::regclass
          AND con.contype = 'u'
          AND array_length(con.conkey, 1) = 1
          AND att.attname = 'email'
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', c.conname);
        RAISE NOTICE 'email UNIQUE 제약 제거: %', c.conname;
    END LOOP;
END
$email$;

ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- ─── 결과 보고 ────────────────────────────────────────────────────────────
\echo
\echo '== 요약 =='
SELECT count(*)                                                      AS 대상_계정,
       count(*) FILTER (WHERE login_id = lower(split_part(email, '@', 1))) AS 그대로,
       count(*) FILTER (WHERE login_id <> lower(split_part(email, '@', 1))
                          AND login_id = candidate)                  AS 규칙으로_변환,
       count(*) FILTER (WHERE login_id <> candidate)                 AS 충돌로_번호_붙음,
       count(*) FILTER (WHERE withdrawn)                             AS 그중_탈퇴계정
FROM login_id_plan;

\echo '== email 앞부분과 달라지는 계정 (탈퇴 안 한 계정만, 최대 50건) =='
SELECT user_id, email, login_id,
       CASE WHEN login_id <> candidate THEN '충돌 → 번호' ELSE '규칙 변환' END AS 사유
FROM login_id_plan
WHERE NOT withdrawn
  AND login_id <> lower(split_part(email, '@', 1))
ORDER BY user_id
LIMIT 50;

\echo '== 시드 계정 =='
SELECT email, login_id
FROM users
WHERE email IN ('test3@example.com') OR email LIKE 'seed.%@example.com'
ORDER BY user_id;

\if :{?apply}
COMMIT;
\echo '>>> 반영 완료 (COMMIT)'
\else
ROLLBACK;
\echo '>>> 드라이런 — 아무것도 바뀌지 않았다 (ROLLBACK). 반영하려면 -v apply=1'
\endif
