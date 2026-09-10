-- 통합 테스트(infra/integration-test.py)가 남긴 데이터를 지운다.
--
-- 대상은 이메일이 integ.%@example.com 인 계정과 거기 딸린 모든 행이다.
-- 그 접두어로 만들어지는 계정은 통합 테스트 말고 다른 경로가 없으므로,
-- 스크립트가 중간에 죽어 정리를 못 했을 때 이 파일만 돌리면 된다.
--
-- 실행:
--   psql "$RAILWAY_DB_URL" -f infra/integration-cleanup.sql
--
-- 시드 데이터(seed.%@example.com, test3@example.com)는 접두어가 달라 건드리지 않는다.
BEGIN;
CREATE TEMP TABLE t_users AS
  SELECT user_id FROM users WHERE email LIKE 'integ.%@example.com';
CREATE TEMP TABLE t_rooms AS
  SELECT room_id FROM drawing_rooms WHERE owner_id IN (SELECT user_id FROM t_users);
CREATE TEMP TABLE t_diaries AS
  SELECT diary_id FROM diaries WHERE room_id IN (SELECT room_id FROM t_rooms)
  UNION
  SELECT diary_id FROM diary_collaborators WHERE user_id IN (SELECT user_id FROM t_users);

DELETE FROM notifications
 WHERE receiver_id IN (SELECT user_id FROM t_users)
    OR sender_id   IN (SELECT user_id FROM t_users)
    OR (type IN ('LIKE','COMMENT') AND target_id IN (SELECT diary_id FROM t_diaries))
    OR (type = 'ROOM_INVITE'       AND target_id IN (SELECT room_id  FROM t_rooms));
DELETE FROM likes    WHERE user_id IN (SELECT user_id FROM t_users) OR diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM comments WHERE user_id IN (SELECT user_id FROM t_users) OR diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM ai_scores           WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM diary_tags          WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM diary_collaborators WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM diaries             WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM room_invites WHERE room_id IN (SELECT room_id FROM t_rooms)
                            OR sender_id IN (SELECT user_id FROM t_users)
                            OR receiver_id IN (SELECT user_id FROM t_users);
DELETE FROM room_members WHERE room_id IN (SELECT room_id FROM t_rooms) OR user_id IN (SELECT user_id FROM t_users);
DELETE FROM drawing_rooms WHERE room_id IN (SELECT room_id FROM t_rooms);
DELETE FROM follows    WHERE follower_id IN (SELECT user_id FROM t_users) OR following_id IN (SELECT user_id FROM t_users);
DELETE FROM categories WHERE user_id IN (SELECT user_id FROM t_users);
DELETE FROM images     WHERE uploader_id IN (SELECT user_id FROM t_users);
-- 이 실행이 만든 태그는 아무 일기도 참조하지 않을 때만 지운다(전역 사전이라 남의 것을 건드리지 않도록).
DELETE FROM tags WHERE name LIKE 'integ-%'
   AND NOT EXISTS (SELECT 1 FROM diary_tags dt WHERE dt.tag_id = tags.tag_id);
DELETE FROM users WHERE user_id IN (SELECT user_id FROM t_users);
COMMIT;
