# 통합 테스트 리포트

- **실행 시각**: 2026-09-11 02:24:10 (KST 기준 로컬 시각)
- **대상 서버**: `https://drawing-diary-production.up.railway.app`
- **결과**: 80 PASS / 0 FAIL (총 80)
- **데이터 정리**: 완료 (DB에서 실제 DELETE)

## 단계별 결과


### 1단계: 가입과 로그인 — ✅ 전부 통과

- ✅ A 회원가입 — `HTTP 201 {"userId": 19, "email": "integ.1789061050.a@example.com", "nickname": "integA1789061050"}`
- ✅ A 로그인 (accessToken·refreshToken) — `HTTP 200 userId=19`
- ✅ B 회원가입 — `HTTP 201 {"userId": 20, "email": "integ.1789061050.b@example.com", "nickname": "integB1789061050"}`
- ✅ B 로그인 (accessToken·refreshToken) — `HTTP 200 userId=20`
- ✅ C 회원가입 — `HTTP 201 {"userId": 21, "email": "integ.1789061050.c@example.com", "nickname": "integC1789061050"}`
- ✅ C 로그인 (accessToken·refreshToken) — `HTTP 200 userId=21`
- ✅ A /api/users/me — 가입 정보와 일치 — `HTTP 200 {"id": 19, "email": "integ.1789061050.a@example.com", "nickname": "integA1789061050", "profileImageUrl": null, "bio": null, "followerCount": 0, "followingCount": 0}`
- ✅ A 프로필 수정 — `HTTP 200 {"id": 19, "nickname": "integA1789061050수정", "profileImageUrl": "https://picsum.photos/seed/integ-a/200/200", "bio": "통합테스트 계정입니다"}`
- ✅ 수정 내용이 재조회에 반영 — `HTTP 200 {"id": 19, "email": "integ.1789061050.a@example.com", "nickname": "integA1789061050수정", "profileImageUrl": "https://picsum.photos/seed/integ-a/200/200", "bio": "통합테스트 계정입니다", "followerCount": 0, "followingCount": 0}`

### 2단계: 소셜 (팔로우 → 목록 → isFollowing → 알림) — ✅ 전부 통과

- ✅ A → B 팔로우 — `HTTP 200 null`
- ✅ B의 팔로워 목록에 A가 있음 — `HTTP 200 [{"userId": 19, "nickname": "integA1789061050수정", "profileImageUrl": "https://picsum.photos/seed/integ-a/200/200"}]`
- ✅ A가 본 B 프로필의 isFollowing == true — `HTTP 200 {"userId": 20, "nickname": "integB1789061050", "profileImageUrl": null, "bio": null, "followerCount": 1, "followingCount": 0, "isFollowing": true}`
- ✅ 타인 프로필 응답에 email이 없음 (문서 명시) — `키: ['userId', 'nickname', 'profileImageUrl', 'bio', 'followerCount', 'followingCount', 'isFollowing']`
- ✅ B가 본 A 프로필의 isFollowing == false (역방향은 팔로우 아님) — `HTTP 200 isFollowing=False`
- ✅ B에게 FOLLOW 알림 도착 — `HTTP 200 FOLLOW 1건 / 전체 1건`
- ✅ FOLLOW 알림의 targetId는 null (문서 명시) — `targetId=None`

### 3단계: 카테고리와 방 — ✅ 전부 통과

- ✅ A 카테고리 생성 — 통합테스트일상 — `HTTP 201 {"categoryId": 31, "name": "통합테스트일상"}`
- ✅ A 카테고리 생성 — 통합테스트여행 — `HTTP 201 {"categoryId": 32, "name": "통합테스트여행"}`
- ✅ A 방 생성 — `HTTP 201 {"roomId": 34}`
- ✅ A가 B를 방에 초대 — `HTTP 200 null`
- ✅ B에게 ROOM_INVITE 알림 도착 (targetId == roomId) — `HTTP 200 ROOM_INVITE 1건`
- ✅ B 방 참여 — `HTTP 200 null`
- ✅ 방 멤버가 A, B 둘 다 — `HTTP 200 members=[19, 20]`

### 4단계: AI 선화 가이드 (AI 서버 콜드스타트로 오래 걸릴 수 있음) — ✅ 전부 통과

- ✅ ai-guide 200 + guides 3종 — `HTTP 200 11.3초 {"guides": [{"style": "1", "styleName": "웹툰형", "imageUrl": "https://drawing-diary-production.up.railway.app/api/images/17", "error": null}, {"style": "2", "styleName": "컬러링북형", "imageUrl": "https://dr`
- ✅ 가이드 이미지 URL 확보 (3/3 성공) — `실패: []`
- ✅   가이드 이미지 접근 가능 (200 + 실제 이미지 바이트) — 웹툰형 — `HTTP 200 JPEG 419275bytes`
- ✅   가이드 이미지 접근 가능 (200 + 실제 이미지 바이트) — 컬러링북형 — `HTTP 200 JPEG 292486bytes`
- ✅   가이드 이미지 접근 가능 (200 + 실제 이미지 바이트) — 고퀄리티 애니메이션형 — `HTTP 200 JPEG 492246bytes`

### 5단계: 임시 저장 → 복구 → 이미지 업로드 → 발행 — ✅ 전부 통과

- ✅ A 임시 저장 — `HTTP 200 {"roomId": 34, "savedAt": "2026-09-10T17:24:35.525399"}`
- ✅ B가 A의 임시 저장분을 복구 — `HTTP 200 title='통합테스트 일기' canvas일치=True`
- ✅ 완성작 이미지 업로드 — `HTTP 201 {"url": "https://drawing-diary-production.up.railway.app/api/images/20"}`
- ✅ 업로드 URL이 https (프록시 뒤 스킴 복원) — `https://drawing-diary-production.up.railway.app/api/images/20`
- ✅ 발행 (title/content 생략 → 임시저장분 사용) — `HTTP 200 {"diaryId": 26}`
- ✅ 발행된 일기의 title/content가 임시저장분과 일치 — `HTTP 200 title='통합테스트 일기'`
- ✅ 카테고리 반영 — `categoryId=31 (기대 31)`
- ✅ 태그 반영 — `tags=['일상', 'integ-1789061050']`
- ✅ 임시저장 canvasData가 일기로 복사됨 — `일치=True`

### 6단계: AI 채점과 랭킹 — ✅ 전부 통과

- ✅ ai-score 200 — `HTTP 200 4.6초 {"diaryId": 26, "relevanceScore": 10, "colorScore": 10, "likeScore": 0, "totalScore": 8, "feedback": "일기 내용과 연관된 요소가 그림에 표현되어 있지 않으며, 단 한 가지 색상만 사용되어 색채 다양성이 매우 떨어집니다."}`
- ✅ totalScore가 공식과 일치 (rel*0.5 + color*0.3 + likeScore*0.2, 반올림) — `응답 8 vs 계산 8 (rel=10 color=10 like=0)`
- ✅ feedback 필드 존재 — `feedback='일기 내용과 연관된 요소가 그림에 표현되어 있지 않으며, 단 한 가지 색상만 사용되어 색채 다양성이 매우 떨'`
- ✅ 전체 랭킹에 이 일기가 있음 — `HTTP 200 랭킹 10건`
- ✅ 내 랭킹에 이 일기가 있음 — `HTTP 200 [{"diaryId": 26, "rank": 10, "totalScore": 8}]`

### 7단계: 좋아요 · 댓글 · 알림 — ✅ 전부 통과

- ✅ B 좋아요 — `HTTP 200 {"diaryId": 26, "liked": true, "likeCount": 1}`
- ✅ 좋아요 후 점수 재조회 — `HTTP 200 {"diaryId": 26, "relevanceScore": 10, "colorScore": 10, "likeScore": 5, "totalScore": 9, "feedback": "일기 내용과 연관된 요소가 그림에 표현되어 있지 않으며, 단 한 가지 색상만 사용되어 색채 다양성이 매우 떨어집니다."}`
- ✅ 좋아요 반영되어 likeScore == 5 — `likeScore=5`
- ✅ totalScore가 재계산됨 (공식 일치) — `total=9 기대=9`
- ✅ 좋아요 전보다 totalScore가 올라감 — `8 → 9`
- ✅ B 댓글 작성 (201) — `HTTP 201 {"id": 31, "content": "통합테스트 댓글", "createdAt": "2026-09-10T17:24:44.629641"}`
- ✅ B 댓글 수정 — `HTTP 200 {"id": 31, "content": "통합테스트 댓글 수정본", "updatedAt": "2026-09-10T17:24:45.140342"}`
- ✅ 댓글 목록에 수정본이 반영 — `HTTP 200 [{"id": 31, "userId": 20, "nickname": "integB1789061050", "content": "통합테스트 댓글 수정본", "createdAt": "2026-09-10T17:24:44.629641"}]`
- ✅ A가 B의 댓글 삭제 시도 → 403 — `HTTP 403 {"message": "본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다: 31"}`
- ✅ 좋아요 목록에 B가 있음 — `HTTP 200 [{"userId": 20, "nickname": "integB1789061050", "profileImageUrl": null}]`
- ✅ A에게 LIKE 알림 도착 — `HTTP 200 이 일기 관련 알림: ['COMMENT', 'LIKE']`
- ✅ A에게 COMMENT 알림 도착 — `이 일기 관련 알림: ['COMMENT', 'LIKE']`

### 8단계: 피드 · 탐색 · 잔디 · 타인 일기 목록 — ✅ 전부 통과

- ✅ B의 피드에 A의 일기가 없음 (B는 A를 팔로우하지 않음) — `HTTP 200 피드 0건`
- ✅ 탐색(PUBLIC)에 A의 일기가 있음 — `HTTP 200 탐색 18건`
- ✅ 랜덤 추천에서 A의 일기가 뽑힘 (최대 6회 시도)
- ✅ A의 활동 잔디에 일기 작성일이 찍힘 (서버 저장 시각 기준) — `HTTP 200 서버기준 2026-09-10 / 응답 [{"date": "2026-09-10", "count": 1}]`
- ✅ B가 본 A의 일기 목록에 이 일기가 있음 — `HTTP 200 1건`
- ✅ 목록 응답에 tags가 포함 (피드와 같은 형식) — `tags=[{'tagId': 1, 'name': '일상'}, {'tagId': 23, 'name': 'integ-1789061050'}]`

### 9단계: 공개 범위별 접근 제어 — ✅ 전부 통과

- ✅ A가 PRIVATE으로 변경 — `HTTP 200 {"id": 26, "title": "통합테스트 일기", "updatedAt": "2026-09-10T17:24:50.440502", "tags": [{"tagId": 1, "name": "일상"}, {"tagId": 23, "name": "integ-1789061050"}]}`
- ✅ 제3자 C 조회 → 403 — `HTTP 403 {"message": "비공개 일기입니다: 26"}`
- ✅ 협업자 B는 PRIVATE도 조회 가능 → 200 (발행 시 방 멤버가 협업자로 복사됨) — `HTTP 200 {"id": 26, "title": "통합테스트 일기", "content": "임시 저장해둔 본문이 발행 때 그대로 딸려와야 한다.", "thumbnailUrl": "https://drawing-diary-produ`
- ✅ C의 탐색 목록에서 사라짐 — `HTTP 200 탐색 17건`
- ✅ A가 FOLLOWERS_ONLY로 변경 — `HTTP 200 {"id": 26, "title": "통합테스트 일기", "updatedAt": "2026-09-10T17:24:52.371334", "tags": [{"tagId": 1, "name": "일상"}, {"tagId": 23, "name": "integ-1789061050"}]}`
- ✅ C는 A를 팔로우하지 않으므로 여전히 403 — `HTTP 403 {"message": "비공개 일기입니다: 26"}`
- ✅ C → A 팔로우 — `HTTP 200`
- ✅ 팔로우 후 C도 200 — `HTTP 200 title=통합테스트 일기`
- ✅ 팔로우했으므로 C의 피드에 나타남 — `HTTP 200 피드 1건`

### 10단계: 토큰 수명주기 — ✅ 전부 통과

- ✅ refreshToken으로 accessToken 재발급 — `HTTP 200 userId=19`
- ✅ 새 accessToken으로 API 호출 정상 — `HTTP 200`
- ✅ 잘못된 토큰 → 401 TOKEN_INVALID — `HTTP 401 {"code": "TOKEN_INVALID", "message": "유효하지 않은 토큰입니다"}`
- ✅ 토큰 없음 → 401 TOKEN_MISSING — `HTTP 401 {"code": "TOKEN_MISSING", "message": "인증이 필요합니다"}`
- ✅ 로그아웃 200 — `HTTP 200 null`
- ✅ 로그아웃한 refreshToken으로 재발급 → 401 — `HTTP 401 {"code": "TOKEN_INVALID", "message": "유효하지 않은 토큰입니다"}`

### 11단계: 탈퇴 후 동작 — ✅ 전부 통과

- ✅ B 회원 탈퇴 — `HTTP 200 null`
- ✅ 탈퇴한 계정으로 로그인 → 401 — `HTTP 401 {"message": "이메일 또는 비밀번호가 올바르지 않습니다."}`
- ✅ A의 팔로워 목록에서 B가 빠짐 — `HTTP 200 [{"userId": 21, "nickname": "integC1789061050", "profileImageUrl": null}]`
- ✅ 좋아요 목록에서 탈퇴자 B가 빠짐 (문서 명시) — `HTTP 200 목록 0명`
- ✅ 탈퇴해도 likeScore는 유지 (목록과 수가 다른 것이 의도된 동작) — `탈퇴 전 5 → 탈퇴 후 5`

## 관찰 — 실패는 아니지만 알아둘 것

- **[1단계]** POST /api/auth/signup 은 200이 아니라 **201**을 반환한다. docs/API.md에 상태 코드가 적혀 있지 않다.
- **[4단계]** AI 선화 가이드 이미지의 실제 형식은 JPEG다. docs/API.md에 형식이 명시되어 있지 않다(업로드 허용 형식과 별개).
- **[8단계]** ⚠️ **시간대**: 서버가 저장한 작성 시각은 `2026-09-10T17:24:36`(UTC)인데 실행 환경의 오늘은 `2026-09-11`(KST)다. 활동 잔디는 UTC 날짜로 묶이므로, KST 00:00~09:00에 쓴 일기는 사용자에게 **하루 전 칸**에 찍힌다.
- **[9단계]** 9단계의 'B가 조회 → 403' 이라는 시나리오 전제는 성립하지 않는다. B는 3단계에서 방에 참여했고 `submit()`이 발행 시점의 방 멤버 전원을 협업자로 복사하므로, B는 PRIVATE도 볼 수 있는 **공동 작성자**다. 접근 제어는 제3자 C로 판정한다.

## 이 실행이 만든 데이터

> `integ.` 로 시작하는 계정과 거기 딸린 모든 데이터는 **이 스크립트가 만든 테스트용**이다.
> 스크립트가 중간에 죽어 정리가 안 된 경우에도 같은 기준으로 지우면 된다.

- 계정: ['integ.1789061050.a@example.com', 'integ.1789061050.b@example.com', 'integ.1789061050.c@example.com']
- diaryId: [26]
- roomId: [34]
- imageId: [17, 18, 19, 20]
- categoryId: [31, 32]
- commentId: [31]
- 태그 이름: ['integ-1789061050']

### 정리 로그
```
BEGIN
SELECT 3
SELECT 1
SELECT 1
DELETE 5
DELETE 1
DELETE 1
DELETE 1
DELETE 2
DELETE 2
DELETE 1
DELETE 1
DELETE 2
DELETE 1
DELETE 2
DELETE 2
DELETE 4
DELETE 1
DELETE 3
COMMIT
```

## 수동 정리 방법

정리를 건너뛴 경우 아래를 실행하면 `integ.` 계정과 딸린 데이터가 전부 지워진다.

```bash
psql "$RAILWAY_DB_URL" -f infra/integration-cleanup.sql
```
