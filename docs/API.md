# API 명세서

> ### 최근 변경 (AI feedback 노출 + 태그)
>
> | 변경 | 내용 |
> |---|---|
> | 추가 | AI 점수 3종 응답에 **`feedback`** 필드 (`ai_scores.ai_comment`. 비어 있으면 null) |
> | 신설 | `GET /api/tags/search?q=` — 태그 부분 일치 검색 |
> | 확대 | `POST /api/rooms/{roomId}/submit`이 **`tags`**(태그 이름 배열)를 받음 |
> | 확대 | `PATCH /api/diaries/{diaryId}`가 **`tags`** 로 태그를 교체 |
> | 추가 | `GET /api/diaries/{id}`·피드·탐색·랜덤 추천 응답에 **`tags`** 배열 |
>
> **Breaking change 없음.** 전부 추가 필드이고 기존 필드는 그대로입니다. 태그를 쓰지 않는
> 화면은 아무것도 고치지 않아도 되고, 기존 일기는 `tags`가 **빈 배열 `[]`** 로 내려옵니다.
>
> ⚠️ 태그를 붙이는 API는 **따로 없습니다.** 태그는 발행·수정 요청에 이름 배열로 실어 보내면
> 없는 이름이 자동으로 만들어집니다(id가 아니라 **이름**을 보냅니다). 자세한 규칙은
> [Tag](#tag) 참고.
>
> <details>
> <summary>이전 변경 (홈 화면 랜덤 추천)</summary>
>
> ### 최근 변경 (홈 화면 랜덤 추천)
>
> | 변경 | 내용 |
> |---|---|
> | 신설 | `GET /api/explore/random` — `PUBLIC` 일기에서 무작위 N개. 호출할 때마다 다른 조합 |
>
> **Breaking change 없음.** 기존 엔드포인트의 요청·응답 형식은 그대로입니다.
>
> ⚠️ 이 경로만 **페이지네이션이 없습니다**(`cursor` 없음, `limit` 상한도 50이 아니라 **20**).
> 홈 화면 카드 섹션처럼 "새로고침하면 다른 추천"에 쓰는 경로라, 무한스크롤에는 기존
> [`GET /api/explore`](#추천-피드-탐색)를 그대로 쓰세요.
>
> <details>
> <summary>이전 변경 (AI 서버 연동)</summary>
>
> ### 최근 변경 (AI 서버 연동)
>
> | 변경 | 내용 |
> |---|---|
> | 신설 | `POST /api/rooms/{roomId}/ai-guide` — 선화 가이드 3종을 **병렬** 생성 |
> | 신설 | `POST /api/diaries/{diaryId}/ai-score` — 완성 이미지를 AI에 보내 점수 자동 산정 |
> | 확대 | `POST /api/images`가 **`image/webp`** 도 허용 (AI 서버와 형식 집합을 맞춤) |
> | 설정 | AI 서버 주소를 `AI_SERVER_URL` 환경변수로 분리 (기본값 배포 주소) |
>
> **Breaking change 없음.** 기존 엔드포인트의 요청·응답 형식은 그대로입니다.
>
> ⚠️ AI 서버가 Render 무료 티어라 **유휴 상태에서 깨어나는 데만 50초 안팎**이 걸립니다
> (실측 52초). 프론트는 첫 호출에 최소 90초 이상의 타임아웃과 로딩 UI를 두세요.
>
> <details>
> <summary>이전 변경 (피드 팔로우 판정 확대 + 랭킹 API)</summary>
>
> | 변경 | 내용 |
> |---|---|
> | 확대 | `GET /api/feed`가 **협업자 중 누구라도 팔로우 중이면** 포함. 예전에는 작성자(방장)만 봤음 |
> | 신설 | `POST`·`GET /api/diaries/{diaryId}/scores` (AI 점수 저장·조회) |
> | 신설 | `GET /api/rankings`, `GET /api/rankings/me` |
> | 변경 | 랭킹 총점을 **백엔드가 계산**. AI 서버는 `relevanceScore`·`colorScore`만 반환 |
> | 폐기 | `ai_scores.theme_score` — 통합 랭킹 전환으로 미사용(항상 0) |
>
> **Breaking change 없음.** 피드 응답 형식은 그대로고 `user`도 여전히 작성자(첫 협업자)입니다.
> 피드에 나오는 일기가 늘어날 뿐입니다.
>
> ⚠️ `GET /api/rankings`는 피드·탐색과 달리 **`cursor`가 아니라 `offset`** 을 씁니다. 이유는
> [전체 랭킹](#전체-랭킹) 참고.
>
> <details>
> <summary>이전 변경 (이미지 업로드 API + Room 작업 상태 임시 저장)</summary>
>
> **Breaking change 없음.**
>
> | 변경 | 내용 |
> |---|---|
> | 신설 | `POST /api/images` (업로드), `GET /api/images/{imageId}` (조회, **인증 불필요**) |
> | 신설 | `PUT /api/rooms/{roomId}/canvas` (작업 상태 임시 저장), `GET /api/rooms/{roomId}/canvas` (복구) |
> | 완화 | `POST /api/rooms/{roomId}/submit`의 `title`·`content`가 **선택**이 됨. 생략하면 방에 임시 저장해둔 값이 쓰임 |
> | 제거 | `PUT /api/diaries/{diaryId}/canvas` (미구현 상태로만 적혀 있던 항목) → Room 쪽으로 대체 |
>
> 기존처럼 `submit`에 `title`·`content`를 직접 실어 보내는 클라이언트는 그대로 동작합니다. 빈 문자열을 보내는 경우도 예전과 같이 400입니다.
>
> <details>
> <summary>이전 변경 (Diary 응답 필드명 통일 + Category 신설)</summary>
>
> **Breaking change 없음 — 기존 필드는 전부 유지됩니다.** 새 이름을 추가하고 옛 이름을 남겨두는 방식이라, 지금 붙어 있는 프론트는 고치지 않아도 그대로 동작합니다.
>
> | 경로 | 추가된 필드 | deprecated (값은 그대로 내려감) |
> |---|---|---|
> | `GET /api/diaries/{id}` | `content`, `thumbnailUrl`, `categoryId`, `categoryName` | `textContent` → `content` |
> | `GET /api/diaries/my` | `content`, `categoryId`, `categoryName` | — |
> | `GET /api/feed`, `GET /api/explore` | `id`, `content`, `thumbnailUrl`, `createdAt`, `categoryId`, `categoryName` | `diaryId` → `id`, `img` → `thumbnailUrl` |
>
> deprecated 필드는 프론트 마이그레이션이 끝난 뒤 제거할 예정입니다. 제거 시점은 프론트와 합의 후 결정.
>
> ⚠️ **`PATCH /api/diaries/{id}`의 요청 바디는 아직 `textContent`입니다** (`content` 아님). 응답만 이름을 맞췄고 요청은 건드리지 않았는데, 요청 필드를 바꾸면 옛 이름으로 보내던 클라이언트의 수정이 **조용히 무시**되기 때문입니다(부분 수정이라 모르는 필드는 "안 바꿈"으로 처리됨). 요청 쪽도 통일이 필요하면 별도로 요청해 주세요.
>
> </details>
>
> </details>
>
> </details>
>
> </details>
>
> </details>

## 인증
로그인과 회원가입, 로그아웃, 토큰 재발급을 제외한 백엔드 API는 Header에 JWT(accessToken) 포함 필요
Authorization: Bearer {accessToken}
실시간 협업 연결 시에도 JWT 인증 필요

accessToken은 발급 후 1시간(3600000ms), refreshToken은 7일 후 만료됨. accessToken이 만료되면 `POST /api/auth/refresh`로 재발급받는다.
refreshToken을 Authorization 헤더로 사용해 API를 호출할 수는 없음(401 `TOKEN_INVALID`). 재발급과 로그아웃 시에만 사용.

### 인증 실패 시 응답 형식
인증이 필요한 API를 유효한 accessToken 없이 호출하면 **401**과 함께 아래 형식의 JSON이 내려온다.

```
HTTP/1.1 401
Content-Type: application/json;charset=UTF-8

{"code":"TOKEN_EXPIRED","message":"토큰이 만료되었습니다"}
```

`code`로 실패 사유를 구분한다. 프론트는 **`code` 값만 보고 분기**하면 된다 (`message`는 사용자 노출용이라 문구가 바뀔 수 있음).

| code | HTTP | message | 발생 상황 | 프론트 대응 |
|---|---|---|---|---|
| `TOKEN_MISSING` | 401 | 인증이 필요합니다 | Authorization 헤더 없음 | 로그인 화면으로 |
| `TOKEN_EXPIRED` | 401 | 토큰이 만료되었습니다 | accessToken의 exp 경과 | **refresh 호출 후 원요청 재시도** |
| `TOKEN_INVALID` | 401 | 유효하지 않은 토큰입니다 | 서명 불일치·변조, JWT 형식 오류, refreshToken을 access 자리에 사용 | 로그인 화면으로 |

자동 재발급 흐름:
```
API 호출 → 401 + code=TOKEN_EXPIRED
  → POST /api/auth/refresh { refreshToken }
      → 200 → 새 accessToken 저장 후 원요청 재시도
      → 401 → 저장된 토큰 폐기하고 재로그인 유도
```

⚠️ 주의할 점
- 인가(권한) 실패는 여전히 **403**이고 본문 형식도 다르다(`{ message }`). 즉 **401 = 인증 문제(토큰), 403 = 권한 문제**로 확실히 갈린다. 남의 PRIVATE 일기 조회, 방 비멤버 접근 등이 403이다.
- 401 응답에 `WWW-Authenticate` 헤더는 붙지 않는다.
- 만료된 refreshToken을 access 자리에 쓰면 `TOKEN_EXPIRED`가 나온다(만료가 타입 검사보다 먼저 걸림). 이 경우 refresh를 시도해도 401이 나므로, refresh 실패 시 재로그인으로 빠지는 처리가 반드시 필요하다.

## Auth
### 회원가입
- POST /api/auth/signup
- Body: { email, password, nickname }
- Response **201**: { userId, email, nickname }
  - 201이다(200이 아님). 자원을 새로 만드는 경로라 댓글 작성·방 생성·카테고리 생성과 같은 규칙.

### 로그인
- POST /api/auth/login
- Body: { email, password }
- Response: { accessToken, refreshToken, userId }
- ⚠️ Breaking change: 응답에 `refreshToken` 필드가 추가됨. 로그인 시 accessToken과 함께 반드시 저장해둘 것 (로그아웃 시 필요).

### 토큰 재발급
- POST /api/auth/refresh
- **인증 헤더 불필요** (accessToken이 만료된 상태에서 호출하는 경로)
- Body: { refreshToken }
- Response 200: { accessToken, userId }
```json
// 요청
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }

// 응답
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "userId": 82 }
```
- **refreshToken은 재발급되지 않는다.** 기존 refreshToken을 계속 보관해서 쓰면 된다. (재발급 응답이 유실됐을 때 클라이언트가 두 토큰을 모두 잃는 상황을 막기 위함)
- 서명이 유효해도 서버(Redis)에 저장된 refreshToken과 다르면 401 `TOKEN_INVALID`. 로그아웃·회원탈퇴로 폐기된 토큰은 만료 전이라도 재발급에 쓸 수 없다.
- 만료된 refreshToken → 401 `TOKEN_EXPIRED` (재로그인 필요)
- refreshToken 자리에 accessToken을 넣거나 형식이 잘못된 경우 → 401 `TOKEN_INVALID`
- refreshToken이 빈 문자열이면 400 `{ "message": "리프레시 토큰을 입력해주세요." }`

### 로그아웃
- POST /api/auth/logout
- Body: { refreshToken }
- Response: 200 OK (본문 없음)
- 이미 만료/무효한 토큰으로 호출해도 항상 200 반환(멱등). 클라이언트는 로그아웃 성공 여부와 무관하게 로컬에 저장된 토큰을 지우면 됨.

## User
### 내 정보 조회
- GET /api/users/me
- Response: { id, email, nickname, profileImageUrl, bio, followerCount, followingCount }
```json
{
  "id": 82,
  "email": "user@example.com",
  "nickname": "그림쟁이",
  "profileImageUrl": "https://picsum.photos/seed/av1/200",
  "bio": "매일 그림 그리는 사람",
  "followerCount": 10,
  "followingCount": 5
}
```
- `bio`는 아직 설정하지 않았으면 `null`.
- `followerCount`/`followingCount`는 저장된 값이 아니라 요청 시점에 집계한 값이다. 탈퇴한 계정은 카운트에서 빠지므로, 이 숫자는 `/api/users/{userId}/followers`·`/followings` 목록의 길이와 항상 일치한다.

### 프로필 수정
- PATCH /api/users/me
- Body: { nickname, profileImageUrl, bio } — **세 필드 모두 선택적**
- Response: { id, nickname, profileImageUrl, bio }
```json
// 요청: bio만 수정 (nickname, profileImageUrl은 기존 값 유지)
{ "bio": "안녕하세요, 그림일기 씁니다." }

// 응답
{
  "id": 82,
  "nickname": "그림쟁이",
  "profileImageUrl": "https://picsum.photos/seed/av1/200",
  "bio": "안녕하세요, 그림일기 씁니다."
}
```
- 보낸 필드만 반영되고, **보내지 않은 필드는 기존 값이 그대로 유지**된다. 빈 바디 `{}`도 200이며 아무것도 바뀌지 않는다.
- 값을 **비우려면 빈 문자열** `""`을 보낸다 (예: `{"bio": ""}`). 필드를 생략하는 것과 구분된다.
- `nickname`을 보냈는데 빈 문자열이거나 공백뿐이면 **400** `{ "message": "닉네임을 입력해주세요." }`. 단, 아예 보내지 않는 것은 정상이다.
- `nickname` 최대 30자, `profileImageUrl` 최대 500자, `bio` 최대 500자 — 초과 시 400.
- 다른 사람이 쓰는 닉네임으로 바꾸려 하면 409.

### 회원 탈퇴
- DELETE /api/users/me
- Response: 200 OK (본문 없음)
- 탈퇴 즉시 refreshToken이 서버에서 폐기되고, 재로그인이 불가능해짐(401). 이미 발급된 accessToken은 만료 전까지도 이후 요청에서 404(사용자를 찾을 수 없음) 처리됨.

### 사용자 검색
- GET /api/users/search?keyword={검색어}
- Response: [{ id, nickname, profileImageUrl }]

### 타인 정보 조회
- GET /api/users/{userId}
- 인증 필요
- Response 200: { userId, nickname, profileImageUrl, bio, followerCount, followingCount, isFollowing }
```json
{
  "userId": 5,
  "nickname": "유나",
  "profileImageUrl": "https://picsum.photos/seed/dd-user-b/200/200",
  "bio": "카페랑 창밖 고양이를 주로 그려요. 색연필파.",
  "followerCount": 4,
  "followingCount": 2,
  "isFollowing": true
}
```
- **`email`이 없다.** 내 정보 조회(`/api/users/me`)와의 유일한 차이가 이것과 `isFollowing`이다. 남의 이메일은 화면에 쓸 데가 없고, 한 번 내려주면 어느 계정이 어떤 주소를 쓰는지 누구나 수집할 수 있다.
- 응답 키가 `id`가 아니라 **`userId`**임에 주의(`/api/users/me`는 `id`).
- `isFollowing`: **요청자가** 이 사람을 팔로우 중인지. 팔로우 버튼을 "팔로우"로 그릴지 "팔로잉 중"으로 그릴지 정하는 값이라, 프로필 진입 시 별도 요청 없이 바로 쓸 수 있게 여기 담았다.
- 자기 자신을 조회해도 200이며 `isFollowing`은 **항상 false**(자기 팔로우는 불가능).
- 404: 없는 사용자이거나 **탈퇴한 사용자**. 둘을 구분하지 않는데, 구분해 알려주면 어떤 계정이 가입했다가 탈퇴했는지가 드러난다.

> **경로 주의**: `{userId}`는 **숫자만** 매칭된다(`{userId:\d+}`). `/api/users/me`,
> `/api/users/search`처럼 문자로 시작하는 경로가 이 핸들러로 새지 않게 하려는 것이다.
> `/api/users/abc`는 404다.

### 타인 일기 목록 조회
- GET /api/users/{userId}/diaries?cursor={diaryId}&limit=10
- 인증 필요
- 그 사용자가 **협업자로 참여한** 일기 중 요청자가 볼 수 있는 것만
- `limit` 기본 10, 최대 50 (벗어나면 조용히 보정). `cursor`는 "이 diaryId보다 작은 것", 생략하면 첫 페이지
- Response 200: [팔로잉 피드](#팔로잉-피드)와 **완전히 같은 형식** — `{ id, title, content, thumbnailUrl, createdAt, categoryId, categoryName, user, tags, diaryId, img }`
- 정렬은 `diaryId` 내림차순(최신순). 더 없으면 빈 배열 `[]`

공개 범위별로 이렇게 걸러진다.

| visibility | 보이는 사람 |
|---|---|
| `PUBLIC` | 로그인한 누구나 |
| `FOLLOWERS_ONLY` | 협업자이거나, **그 일기의 작성자**를 팔로우하는 사람 |
| `PRIVATE` | 협업자만 |

> **`FOLLOWERS_ONLY` 판정은 "목록 주인"이 아니라 "작성자" 기준이다**
> 목록 주인이 방장이 아닌 협업자로 참여한 일기라면 작성자는 제3자다. 판정을 "목록 주인을
> 팔로우"로 넓히면 작성자를 팔로우하지 않은 사람에게도 카드가 뜨는데, 정작 탭해서 열면
> [일기 상세 조회](#일기-상세-조회)가 403을 낸다. **목록에 뜬 일기는 항상 열 수 있어야 한다**는
> 규칙을 지키려고 상세 조회와 같은 기준을 쓴다(팔로잉 피드도 같은 이유로 같은 선택을 했다).
> 목록 주인이 곧 작성자인 흔한 경우에는 둘이 같은 결과다.

- 자기 자신을 조회하면 협업자 판정이 전부 통과해 **PRIVATE까지 전부** 보인다. 결과적으로 `/api/diaries/my`와 같은 집합이며, 차이는 응답 형식과 페이지네이션 유무뿐이다.

### 활동 잔디 (달력)
- GET /api/users/{userId}/activity?year=2026&month=9
- 인증 필요
- `year`·`month` **둘 다 필수**
- Response 200: `[{ date, count }]`
```json
[
  { "date": "2026-09-01", "count": 2 },
  { "date": "2026-09-05", "count": 1 }
]
```
- `date`는 `yyyy-MM-dd`, 오름차순
- **일기가 없는 날은 배열에 없다.** 프론트가 빈 칸으로 그리면 된다 — 한 달치 0을 다 내려주면 응답의 대부분이 `count: 0`이 된다

> **날짜 기준은 KST(Asia/Seoul)다**
> `year`·`month`도, `date`로 묶는 기준도 전부 KST다. 즉 `year=2026&month=9`는
> **KST 9월 1일 00:00 ~ 9월 30일 23:59:59**에 쓴 일기를 뜻한다.
>
> 서버는 `created_at`을 UTC로 저장하는데, 그대로 잘라 내려주면 **KST 새벽 0~9시에 쓴
> 일기가 전날 칸에 찍힌다**(UTC로는 아직 전날이므로). 잔디는 서버가 이미 날짜로 묶어
> 보내기 때문에 프론트가 이걸 되돌릴 수 없어서, 서버에서 KST로 옮긴 뒤 자른다.
>
> 기준 시간대는 설정값이다(`app.time-zone.display`, 기본 `Asia/Seoul`). 다른 지역을
> 지원하게 되면 이 값만 바꾸면 되고, 저장된 데이터는 건드리지 않는다.
>
> **`createdAt`을 그대로 내려주는 다른 API는 이 변환을 타지 않는다.** 피드·일기 상세의
> `createdAt`은 UTC이므로 프론트가 로컬 시간대로 바꿔서 표시하면 된다.
- `count`는 **요청자가 볼 수 있는 일기만** 센다. 판정 기준은 위 [타인 일기 목록 조회](#타인-일기-목록-조회)와 **완전히 동일**하므로, 잔디에 찍힌 날에는 반드시 목록에 그만큼의 일기가 있다
- 400: `year`가 2000~2100 밖 / `month`가 1~12 밖 / 둘 중 하나라도 없음 / 숫자가 아님
  - 피드의 `limit`처럼 조용히 보정하지 않는다. 잘못된 달을 이번 달로 바꿔치면 프론트가 요청한 것과 다른 달의 잔디를 그리고, 화면상으로는 아무 이상이 없어 보인다

## Follow
### 팔로우
- POST /api/users/{userId}/follow
- Response: 200 OK (본문 없음)
- 400: 자기 자신을 팔로우한 경우
- 404: {userId} 사용자가 없거나 탈퇴한 경우
- 409: 이미 팔로우 중인 경우

### 언팔로우
- DELETE /api/users/{userId}/follow
- Response: 200 OK (본문 없음)
- 팔로우 중이 아니거나 없는 사용자여도 항상 200(멱등). 클라이언트는 응답과 무관하게 UI를 언팔로우 상태로 두면 됨.

### 팔로워 목록 조회
- GET /api/users/{userId}/followers
- Response: [{ userId, nickname, profileImageUrl }]
- 404: {userId} 사용자가 없거나 탈퇴한 경우
- 최신 팔로우 순 정렬. 탈퇴한 사용자는 목록에서 제외됨.

### 팔로잉 목록 조회
- GET /api/users/{userId}/followings
- Response: [{ userId, nickname, profileImageUrl }]
- 404: {userId} 사용자가 없거나 탈퇴한 경우
- 최신 팔로우 순 정렬. 탈퇴한 사용자는 목록에서 제외됨.

## Room
방(Room)은 그림일기를 함께 그리는 작업 공간. `방 생성 → 초대 → 참여 → 같이 그리기 → 발행(submit)` 순서로 진행되고, **Diary는 submit 시점에만 생성됨**(방 없이 일기를 만드는 경로는 없음).

방 상태: `WAITING`(생성 직후) / `DRAWING` / `FINISHED`(발행 완료)
초대 상태: `PENDING` / `ACCEPT` / `REJECT`

### 방 생성
- POST /api/rooms
- Body 없음
- Response 201: { roomId }
- 생성자가 owner가 되고, 동시에 방 멤버로도 자동 등록됨. 상태는 `WAITING`으로 시작.

### 방 정보 조회
- GET /api/rooms/{roomId}
- Response 200: { roomId, status, ownerId, members: [{ userId, nickname, profileImageUrl }] }
- 403: 방 멤버가 아닌 경우
- 404: 방이 없거나 삭제된 경우
- `members`는 참여 순(owner가 항상 첫 번째). 탈퇴한 사용자는 목록에서 제외됨.

### 방 삭제
- DELETE /api/rooms/{roomId}
- Response: 200 OK (본문 없음)
- 403: owner가 아닌 경우
- 404: 방이 없거나 이미 삭제된 경우
- Soft delete(`deleted_at`). 삭제 후에는 모든 방 API가 404를 반환하지만, 이미 발행된 일기는 그대로 남음.

### 친구 초대
- POST /api/rooms/{roomId}/invite
- Body: { invitedUserIds: [2, 3] }
- Response: 200 OK (본문 없음)
- 400: `invitedUserIds`가 비어 있는 경우
- 403: 방 멤버가 아닌 경우 (멤버면 owner가 아니어도 초대 가능)
- 404: 방이 없거나, `invitedUserIds`에 없는 사용자가 포함된 경우
- 이미 방 멤버이거나 이미 `PENDING` 초대가 있는 유저는 **에러 없이 조용히 스킵**됨. 클라이언트는 친구 목록에서 고른 대상을 그대로 보내면 되고, 중복 여부를 미리 걸러낼 필요 없음.

### 방 참여
- POST /api/rooms/{roomId}/join
- Response: 200 OK (본문 없음)
- 403: 해당 방에 대한 `PENDING` 초대가 없는 경우
- 404: 방이 없거나 삭제된 경우
- 참여 시 방 멤버로 추가되고 초대 상태가 `ACCEPT`로 바뀜. 이미 멤버면 아무것도 하지 않고 200(멱등) — 재요청해도 403이 되지 않음.

### 방 나가기
- DELETE /api/rooms/{roomId}/leave
- Response: 200 OK (본문 없음)
- 400: owner가 나가려는 경우 (owner는 나갈 수 없고 방 삭제만 가능)
- 404: 방이 없거나 삭제된 경우
- 멤버가 아닌 상태로 호출해도 200(멱등).

### 작업 상태 임시 저장
- PUT /api/rooms/{roomId}/canvas
- Body: { canvasData, title, content } — **세 필드 모두 선택**
  - `canvasData`: 캔버스 스냅샷을 Base64로 인코딩한 문자열. 서버는 디코딩해서 바이너리로 보관하고, 조회 시 다시 Base64로 돌려줌
  - `title`: 100자 이하 / `content`: 길이 제한 없음
- Response 200: { roomId, savedAt }
- 400: `canvasData`가 Base64가 아니거나 `title`이 100자 초과
- 403: 방 멤버가 아닌 경우
- 404: 방이 없거나 삭제된 경우
- 409: 이미 `FINISHED`인 방
- **보낸 필드만 덮어쓰고, 보내지 않은 필드는 기존 값이 유지됨.** 자동 저장이 캔버스만 반복해서 보내도 이미 저장해둔 제목·본문은 지워지지 않음.
- 저장 주체는 멤버 누구나. 방 하나에 상태가 하나뿐이라 마지막 저장이 이깁니다(last-write-wins).

### 작업 상태 조회
- GET /api/rooms/{roomId}/canvas
- Response 200: { roomId, canvasData, title, content, updatedAt }
  - 한 번도 저장한 적이 없으면 `canvasData`·`title`·`content`가 전부 null
  - `canvasData`는 Base64 문자열
- 403: 방 멤버가 아닌 경우
- 404: 방이 없거나 삭제된 경우
- 다른 멤버가 저장한 상태도 그대로 내려옴. 새로고침·재접속 후 이어 그리기, 다른 기기에서 이어받기에 쓰면 됨.

### 일기 최종 발행
- POST /api/rooms/{roomId}/submit
- Body: { title, content, finalImg, visibility, categoryId, canvasData, tags }
  - `visibility`만 필수(`PUBLIC` | `FOLLOWERS_ONLY` | `PRIVATE`)
  - `title`(100자 이하) · `content` · `canvasData`는 **선택**. 생략하면 임시 저장해둔 방의 값이 사용됨
  - `finalImg`는 선택(500자 이하) — `POST /api/images`가 돌려준 URL을 넣으면 됨
  - `categoryId`는 null 허용
  - `tags`는 선택. **태그 이름의 배열**(`["오운완","일상"]`)이며 id가 아님. 없는 이름은 이때 자동 생성됨. 생략하면 태그 없이 발행. 규칙은 [Tag](#tag) 참고
- Response 200: { diaryId }
- 400: 요청에도 방에도 `title` 또는 `content`가 없는 경우, 또는 형식 오류(빈 문자열, 길이 초과, `canvasData`가 Base64가 아님, 태그 50자 초과·11개 이상)
- 403: 방 멤버가 아닌 경우
- 404: 방이 없거나, `categoryId`에 해당하는 카테고리가 없는 경우
- 409: 이미 `FINISHED`인 방에 다시 발행을 요청한 경우
- 동작: ① 값 결정(요청에 있으면 그것, 없으면 방의 임시 저장값) ② 일기 생성(방과 연결) ③ 발행 시점의 방 멤버 전원을 일기 협업자로 복사 ④ 태그 연결(없는 이름은 생성) ⑤ 방 상태를 `FINISHED`로 변경
- 협업자는 **발행 시점 기준으로 고정**됨. 발행 후 방을 나가더라도 이미 만들어진 일기의 협업자 목록은 바뀌지 않음.
- 임시 저장해둔 `canvasData`는 일기로 복사되어 `GET /api/diaries/{id}`의 `canvasData`로 조회됨.

```
방 생성 → (그리는 동안) PUT /canvas 로 주기적 자동 저장
                          ↕ GET /canvas 로 복구
       → POST /submit { visibility } 만 보내도 발행 완료
```

## Image
그림·썸네일 바이너리를 서버 DB에 보관하고 URL로 서빙합니다. 별도 스토리지(S3 등) 없이 동작합니다.

### 이미지 업로드
- POST /api/images
- Content-Type: `multipart/form-data`, 파트 이름 **`file`**
- Response 201: { url }
  - 예: `{ "url": "https://<host>/api/images/12" }` — 이 URL을 그대로 `finalImg`나 프로필 이미지 필드에 넣으면 됨
- 허용 형식: `image/png`, `image/jpeg`, `image/webp` (그 외는 400)
  - AI 서버가 jpeg·png·webp를 받으므로 같은 집합으로 맞췄다
- 최대 10MB (초과 시 413)
- 400: 빈 파일이거나 허용되지 않는 형식
- 401: 미인증 — **업로드는 로그인 필요**
- URL은 요청이 들어온 호스트를 기준으로 만들어짐. 프록시 뒤(Railway 등)에서도 https가 유지되도록 `server.forward-headers-strategy: framework`가 켜져 있음.

### 이미지 조회
- GET /api/images/{imageId}
- **인증 불필요** (`<img src>`에 그대로 넣을 수 있음)
- Response 200: 이미지 바이너리. `Content-Type`은 업로드 시 형식 그대로
- `Cache-Control: max-age=31536000, public, immutable` — 이미지는 수정되지 않으므로 영구 캐시 가능
- 404: 없는 `imageId`
- 삭제 API는 없음. 참조가 끊긴 이미지는 남아 있습니다(현재 정리 배치 없음).

## 개발용 더미 데이터
프론트 화면 개발용 PUBLIC 일기 5건을 특정 계정에 넣는 스크립트: `infra/seed-dummy-diaries.sql`

```bash
psql "$DATABASE_URL" -v target_user_id=2 -f infra/seed-dummy-diaries.sql
```
- `RoomService.submit()`과 같은 순서로 `drawing_rooms`(FINISHED) → `room_members` → `diaries` → `diary_collaborators`를 채운다. 피드는 `diary_collaborators`로 작성자를 찾으므로 이 테이블이 비면 카드에 작성자가 `null`로 나온다.
- 여러 번 실행해도 안전하다(같은 제목이 이미 있으면 건너뜀).
- 생성되는 일기는 모두 `visibility=PUBLIC`이고 `created_at`이 1~15일 전으로 흩어져 있다.

## Feed
홈 화면용 목록 세 개. 셋 다 인증이 필요하고 **응답 형식(카드)이 같다**.
페이지네이션 규칙은 앞의 둘(`/api/feed`·`/api/explore`)만 공유한다 — [랜덤 추천](#랜덤-추천-홈-화면-카드)은 페이지네이션이 없다.

**공통 Response 200**: `[{ id, title, content, thumbnailUrl, createdAt, categoryId, categoryName, user: { userId, nickname, profileImageUrl }, tags, diaryId, img }]`
```json
{
  "id": 83,
  "title": "카테고리 있는 일기",
  "content": "분류가 붙은 일기 본문",
  "thumbnailUrl": "https://picsum.photos/seed/withcat/800/600",
  "createdAt": "2026-08-28T15:05:36.388085",
  "categoryId": 4,
  "categoryName": "맛집 탐방",
  "user": { "userId": 92, "nickname": "someone", "profileImageUrl": null },
  "tags": [{ "tagId": 3, "name": "맛집" }, { "tagId": 7, "name": "주말" }],
  "diaryId": 83,
  "img": "https://picsum.photos/seed/withcat/800/600"
}
```
- `thumbnailUrl`은 `final_img_url`. 저장된 값이 없으면 null.
- `content`는 일기 본문 **전체**(요약·자르기 없음).
- `categoryId`/`categoryName`은 분류가 없으면 **둘 다 null**.
- `user`는 **작성자**(= 협업자 중 가장 먼저 등록된 사람 = 방장). 프로필 이미지를 설정하지 않았으면 `profileImageUrl`은 null.
- `tags`는 카드에 표시할 태그. 태그가 없으면 null이 아니라 **빈 배열 `[]`**(기존 일기는 전부 `[]`). 상세 조회와 같은 값·같은 순서다.
- 페이지 정보를 감싸는 객체 없이 **배열이 그대로** 내려감.
- ⚠️ `diaryId`와 `img`는 **deprecated**. 각각 `id`·`thumbnailUrl`과 같은 값이며 기존 프론트 호환용으로만 남겨둠. 새 화면은 `id`/`thumbnailUrl`을 쓸 것.

**공통 커서 페이지네이션**: `?cursor={diaryId}&limit={n}`
- `cursor`는 **diaryId 기준**. 그 값보다 **작은**(= 더 예전) 일기부터 내려감. 첫 페이지는 `cursor` 생략.
- 다음 페이지는 **직전 응답 마지막 항목의 `id`**(= `diaryId`)를 `cursor`로 넘기면 됨. 응답에 nextCursor 필드는 없음.
- 빈 배열 `[]`이 오면 마지막 페이지.
- 정렬은 `createdAt`이 아니라 **diaryId 내림차순**. 같은 시각에 만들어진 일기가 있어도 순서가 흔들리지 않아 페이지 사이에 누락·중복이 생기지 않음.
- `limit` 기본값 10, 최대 50. 범위를 벗어난 값(0, 음수, 9999 등)은 400이 아니라 **조용히 보정**됨 — 스크롤 중 값 하나 때문에 화면이 비지 않도록.
- 커서는 스냅샷이 아님. 페이지를 넘기는 사이에 새 일기가 올라와도 이미 받은 페이지에는 영향이 없지만, 일기가 삭제되면 그만큼 건수가 줄어들 수 있음.

### 팔로잉 피드
- GET /api/feed?cursor={diaryId}&limit=10
- **내가 팔로우하는 사람이 협업자로 참여한** 일기. 방장이 아니어도 됨.
- 공개 범위별 포함 여부:

| visibility | 피드 포함 |
|---|---|
| `PUBLIC` | 협업자 중 한 명이라도 팔로우 중이면 포함 |
| `FOLLOWERS_ONLY` | **작성자(첫 협업자)를 팔로우해야** 포함. 다른 협업자만 팔로우한 경우는 제외 |
| `PRIVATE` | **제외**. 단 내가 그 일기의 협업자면 포함 |

- 판정 기준은 [일기 상세 조회](#일기-상세-조회)의 조회 권한과 동일. **일기 상세가 403이면 피드에도 안 나옴.**
- 한 일기에서 협업자를 여러 명 팔로우하고 있어도 **피드에는 한 번만** 나옴.
- `user`는 **작성자(첫 생존 협업자)** 로 고정. 내가 팔로우한 사람이 방장이 아니면 카드에 다른
  이름이 뜰 수 있는데, 일기 하나의 대표 작성자는 하나여야 목록·상세·알림이 어긋나지 않기 때문.
  이 자리를 "내가 팔로우한 협업자"로 바꾸면 보는 사람마다 작성자가 달라짐.
- 방장이 탈퇴하면 작성자가 다음 생존 협업자로 넘어감(`user` 필드도 같이 바뀜). 이제는 방장이
  탈퇴해도 남은 협업자를 팔로우 중이면 **PUBLIC 일기가 피드에서 사라지지 않음**.
- 자기 자신은 팔로우할 수 없으므로 **내 일기는 피드에 안 나옴**. 내 일기는 `/api/diaries/my`.
- 아무도 팔로우하지 않으면 빈 배열 `[]`.
- 언팔로우하면 즉시 피드에서 사라짐.

> **`FOLLOWERS_ONLY`만 작성자 기준인 이유**: 포함 조건만 넓히고 공개 범위 규칙은 건드리지 않았다.
> 여기까지 넓히면 작성자가 모르는 사람(공동 작업자의 팔로워)에게 글이 열리는 셈이라, 작성자가
> 직접 고른 공개 범위를 서버가 임의로 완화하게 된다. 지금 규칙이면 피드에 뜬 일기는 항상 탭해서
> 열 수 있다. 이 동작을 바꾸려면 팀에서 공개 범위 정책 자체를 다시 정해야 한다.

### 추천 피드 (탐색)
- GET /api/explore?cursor={diaryId}&limit=10
- `PUBLIC` 일기 **전체**. 팔로우 여부와 무관하므로 누가 호출하든 결과가 같음.
- deprecated된 `GET /api/diaries`와 같은 일기 집합이며, 커서 페이지네이션과 `user` 형태만 다름.
- 홈 화면 카드처럼 "매번 다른 추천"이 필요하면 [랜덤 추천](#랜덤-추천-홈-화면-카드)을 쓸 것.

### 랜덤 추천 (홈 화면 카드)
- GET /api/explore/random?limit=10
- `PUBLIC` 일기 중 **무작위 N개**. 호출할 때마다 조합과 순서가 달라짐.
- 응답 형식은 [`GET /api/explore`](#추천-피드-탐색)와 **완전히 동일**(위의 공통 카드 형태, 필드도 같음).
- 내려갈 수 있는 일기 집합도 `/api/explore`와 **완전히 동일**하다. `PRIVATE`·`FOLLOWERS_ONLY`는 **절대 나오지 않고**, 다른 것은 정렬뿐.
- 팔로우 여부와 무관하므로 누가 호출하든 **후보 집합은 같다**(뽑힌 결과만 매번 다름).

**⚠️ 페이지네이션 없음 — `cursor`를 받지 않음**
- 무한스크롤이 아니라 **"새로고침하면 다른 추천"** 이 목적인 경로다.
- 그래서 **호출 사이의 중복·누락을 보장하지 않는다.** 연속 호출에 같은 일기가 겹쳐 나올 수 있음.
- 목록을 끝까지 넘겨야 하는 화면에는 `/api/explore`(커서 페이지네이션)를 쓸 것.

**`limit`**
- 기본값 10, **최대 20**. 범위를 벗어난 값(0, 음수, 9999 등)은 400이 아니라 **조용히 보정**됨.
- ⚠️ 상한이 `/api/feed`·`/api/explore`(50)와 **다르다**. 홈 카드 한 섹션에 들어갈 만큼만 필요해서 낮게 뒀음.
- 숫자가 아닌 값(`limit=abc`)은 파라미터 변환이 실패해 **400**.
- 조건에 맞는 일기가 `limit`보다 적으면 **있는 만큼만** 내려감.

| 상황 | 응답 |
|---|---|
| `PUBLIC` 43건, `limit=20` | 20건 |
| `PUBLIC` 3건, `limit=20` | **3건** (모자란 만큼 채우지 않음) |
| `PUBLIC` 0건 | 빈 배열 `[]` |
| `limit=0`, `limit=-5` | 10건(기본값) |
| `limit=21`, `limit=9999` | 20건(상한) |

> **성능 메모 — `ORDER BY RANDOM()`**
> 구현은 Postgres의 `ORDER BY RANDOM()`이다. 조건에 맞는 행 **전체**에 난수를 매겨 정렬한 뒤 앞에서
> 자르므로, 인덱스가 있어도 매번 풀 스캔이다. 지금 규모(`PUBLIC` 수십~수천 건)에서는 체감되지 않지만
> 일기가 수십만 건이 되면 **이 경로만** 눈에 띄게 느려진다. 그때는 `TABLESAMPLE`이나 난수 키 컬럼처럼
> 전체를 훑지 않는 방식으로 갈아타야 한다.
> **응답 형식을 바꾸지 않고 내부만 교체할 수 있으므로 프론트가 지금 대비할 것은 없다.**

> **쿼리 수**: 요청 1건당 SQL **4회 고정**(랜덤 id 추출 → 일기+카테고리 → 작성자 → 태그)으로
> `limit`과 무관하다. `/api/explore`(3회)보다 한 번 많은 건 id를 먼저 뽑기 때문이고,
> 건수가 늘어도 함께 늘지 않는다. (태그 응답이 붙기 전에는 각각 3회·2회였다.)

## Diary
일기 **생성 API는 없음**. 일기는 `POST /api/rooms/{roomId}/submit`(방 발행)으로만 만들어지고, 그때 방 멤버 전원이 협업자로 등록됨. 아래 API에서 "협업자"는 `diary_collaborators`에 포함된 사람을 뜻함.

공개 범위: `PUBLIC` / `FOLLOWERS_ONLY` / `PRIVATE`

### 일기 목록 조회 (deprecated)
- GET /api/diaries
- ⚠️ **deprecated — [`GET /api/explore`](#추천-피드-탐색)를 쓸 것.** 페이지네이션이 없어 일기가 늘어나면 응답이 계속 커짐. 이미 붙어 있는 프론트를 깨지 않으려고 남겨둔 경로일 뿐, 새 화면에서는 쓰지 말 것.
- Response 200: [{ id, title, authorId, authorNickname, createdAt, thumbnailUrl, visibility }]
- ⚠️ 이 경로만 필드명이 예전 그대로다(`categoryId`·`content` 없음). deprecated 경로라 손대지 않았음.
- 남의 일기 둘러보기용이라 **`PUBLIC`인 일기만** 내려감. 내 비공개 일기를 보려면 `/api/diaries/my`를 쓸 것.
- 최신순(일기 id 역순) 정렬. `thumbnailUrl`은 `final_img_url`.
- `authorId`/`authorNickname`은 협업자 중 가장 먼저 등록된 사람 = 방장. 협업자가 모두 탈퇴한 경우 두 필드는 null로 내려감.
- `/api/explore`와 **내려가는 일기 집합은 완전히 동일**(같은 조회를 공유함). 응답 필드 이름만 다름.

### 내 일기 목록 조회
- GET /api/diaries/my
- Response 200: [{ id, title, content, thumbnailUrl, createdAt, categoryId, categoryName, visibility, tags }]
- `content`는 본문 전체, `thumbnailUrl`은 `final_img_url`. `categoryId`/`categoryName`은 분류가 없으면 둘 다 null.
- `tags`는 `[{ id, name }]`. 태그가 없으면 null이 아니라 **빈 배열** `[]` — 피드·탐색·상세와 같은 규칙.
- 요청자가 협업자인 일기만. 본인이 참여한 일기이므로 **visibility와 무관하게 전부** 보임.
- 참여한 일기가 없으면 빈 배열 `[]`.

### 일기 상세 조회
- GET /api/diaries/{diaryId}
- Response 200: { id, title, content, thumbnailUrl, imageUrl, createdAt, categoryId, categoryName, visibility, canvasData, tags, textContent }
```json
{
  "id": 83,
  "title": "카테고리 있는 일기",
  "content": "분류가 붙은 일기 본문",
  "thumbnailUrl": "https://picsum.photos/seed/withcat/800/600",
  "imageUrl": "https://picsum.photos/seed/withcat/800/600",
  "createdAt": "2026-08-28T15:05:36.388085",
  "categoryId": 4,
  "categoryName": "맛집 탐방",
  "visibility": "PUBLIC",
  "canvasData": null,
  "tags": [{ "tagId": 3, "name": "맛집" }, { "tagId": 7, "name": "주말" }],
  "textContent": "분류가 붙은 일기 본문"
}
```
  - `content`는 `diaries.content`. **`textContent`는 deprecated**이며 `content`와 항상 같은 값 — 기존 프론트 호환용으로만 남겨둠.
  - `thumbnailUrl`과 `imageUrl`은 **둘 다 `final_img_url`**이라 현재 항상 같은 값. 목록 카드와 상세 화면이 같은 컴포넌트를 공유할 수 있도록 두 이름으로 함께 내려감. 원본/축소본을 따로 저장하게 되면 그때 갈라짐.
  - `categoryId`/`categoryName`은 `diaries.category_id`가 없으면 **둘 다 null**. 있으면 `categories`를 조인해 이름까지 채워짐.
  - `canvasData`는 BYTEA라 **Base64 문자열로 인코딩**해서 내려감. 저장된 값이 없으면 null.
  - `tags`는 일기에 달린 태그. 태그가 없으면 null이 아니라 **빈 배열 `[]`**. 정렬은 태그 이름순이라 같은 일기를 다시 조회해도 순서가 흔들리지 않음.
- 403: 아래 공개 범위별 조건을 만족하지 못한 경우
- 404: 일기가 없거나 이미 삭제된 경우

공개 범위별 조회 권한:

| visibility | 조회 가능한 사람 |
|---|---|
| `PUBLIC` | 로그인한 누구나 |
| `FOLLOWERS_ONLY` | 협업자, **또는 작성자(`authorId`)를 팔로우하는 사람** |
| `PRIVATE` | 협업자만 |

- `FOLLOWERS_ONLY`의 팔로우 판정 기준은 **작성자 한 명**(= 협업자 중 첫 번째 = 방장). 작성자가 아닌 다른 협업자를 팔로우하는 것으로는 열리지 않음.
- 협업자 여부를 먼저 보므로, 협업자는 작성자를 팔로우하지 않아도 항상 조회 가능(작성자 본인 포함).
- 팔로우는 단방향. 작성자가 나를 팔로우하는 것은 권한이 되지 않음.
- 언팔로우하면 즉시 다시 403이 됨.
- 협업자가 모두 탈퇴해 작성자를 특정할 수 없으면 팔로우를 확인할 대상이 없으므로 403.
- 목록 API(`GET /api/diaries`)는 여전히 `PUBLIC`만 내려감 — 팔로우 중이어도 `FOLLOWERS_ONLY` 일기는 목록에 나오지 않고, 상세 조회로만 접근 가능.

### 일기 수정
- PATCH /api/diaries/{diaryId}
- Body: { title, textContent, visibility, tags } — **넷 다 선택**. 보내지 않거나 null인 필드는 기존 값을 유지(부분 수정).
- Response 200: { id, title, updatedAt, tags }
- 400: `title`이 빈 문자열이거나 100자 초과, `textContent`가 빈 문자열, `visibility`가 정의되지 않은 값, 태그가 50자 초과이거나 11개 이상인 경우
- 403: 협업자가 아닌 경우
- 404: 일기가 없거나 이미 삭제된 경우
- 협업자면 누구나 수정 가능(방장 전용 아님). 빈 body `{}`를 보내면 아무것도 바뀌지 않고 200 — 이때는 `updatedAt`도 갱신되지 않음.

**`tags` 규칙 (다른 필드와 같은 "null = 안 바꿈")**

| 보낸 값 | 결과 |
|---|---|
| 필드 생략 또는 `null` | 태그를 **건드리지 않음**(기존 유지) |
| `["a","b"]` | 기존 태그를 버리고 **통째로 교체**(추가가 아님) |
| `[]` | 태그를 **전부 제거** |

- 응답의 `tags`는 **수정 후 현재 값 전체**다. 태그를 보내지 않아 그대로 둔 경우에도 현재 값이 내려오므로, 프론트는 응답만 보고 화면을 다시 그리면 된다.
- 태그만 바꾸는 요청도 유효하다. 다만 `diaries` 행이 그대로라 이때는 **`updatedAt`이 갱신되지 않는다.**
- 태그 이름 규칙(자동 생성·중복 처리·상한)은 [Tag](#tag)와 완전히 동일하다 — 발행과 수정이 같은 로직을 탄다.

### 일기 삭제
- DELETE /api/diaries/{diaryId}
- Response 200: { "message": "일기가 삭제되었습니다" }
- 403: 협업자가 아닌 경우
- 404: 일기가 없거나 이미 삭제된 경우
- ⚠️ **하드 삭제**. `diaries` 테이블에 `deleted_at` 컬럼이 없어 soft delete를 쓸 수 없음. 삭제 시 해당 일기의 협업자·댓글·좋아요·AI 점수·태그 연결이 **함께 영구 삭제**되며 복구 불가. 방(`drawing_rooms`)은 그대로 남음.
- 협업자면 누구나 삭제 가능하므로, 클라이언트에서 삭제 확인 절차를 두는 것을 권장.

### 캔버스 저장
일기 단위 캔버스 저장 API는 없습니다. 캔버스는 발행 전 **방** 단위로 저장하며(`PUT /api/rooms/{roomId}/canvas`), 발행 시 일기로 복사됩니다. 발행된 일기의 `canvasData`는 읽기 전용입니다.

## Collaboration
### 협업자 초대
- POST /api/diaries/{diaryId}/participants
- Body: { userId }
- Response: { diaryId, userId, status }

### 받은 초대 목록 조회
- GET /api/invitations
- Response: [{ invitationId, diaryId, diaryTitle, inviterNickname, status }]

### 초대 수락
- POST /api/invitations/{invitationId}/accept
- Response: { invitationId, status }

### 초대 거절
- POST /api/invitations/{invitationId}/reject
- Response: { invitationId, status }

### 협업자 목록 조회
- GET /api/diaries/{diaryId}/participants
- Response: [{ userId, nickname, role }]

### 협업자 삭제
- DELETE /api/diaries/{diaryId}/participants/{userId}
- Response: { message }

## Tag
태그는 **전역 사전**이다. `tags`에 이름이 유일하게 한 번만 존재하고, 어떤 일기가 그 태그를
쓰는지는 `diary_tags`가 들고 있다. 사용자별 태그가 아니므로 남이 만든 태그도 그대로 검색되고
재사용된다(사용자별로 나뉘는 [Category](#category)와 다른 점).

**태그를 만들거나 붙이는 전용 API는 없다.** 일기를 발행하거나 수정할 때 이름 배열을 실어
보내면 없는 이름이 그때 만들어지고 일기에 연결된다.

| 하고 싶은 것 | 쓰는 API |
|---|---|
| 태그 검색(자동완성) | `GET /api/tags/search?q=` |
| 발행하면서 태그 달기 | [`POST /api/rooms/{roomId}/submit`](#일기-최종-발행)의 `tags` |
| 나중에 태그 바꾸기·지우기 | [`PATCH /api/diaries/{diaryId}`](#일기-수정)의 `tags` |
| 일기에 달린 태그 보기 | [`GET /api/diaries/{diaryId}`](#일기-상세-조회)·피드·탐색·랜덤 추천의 `tags` |

### 태그 검색
- GET /api/tags/search?q={keyword}
- 인증 필요
- Response 200: `[{ tagId, name }]`
```json
[{ "tagId": 3, "name": "Cat" }, { "tagId": 12, "name": "고양이" }]
```
- **부분 일치**(앞뒤 어디에 있어도 매치) · **대소문자 무시**. `q=cat`과 `q=CAT`은 같은 결과.
- `q`가 없거나 공백뿐이면 **빈 배열 `[]`**. 400이 아니다 — 입력창이 비어 있을 때도 자동완성이
  그대로 호출되는 경로라 "아직 검색어가 없음"은 오류가 아니다. 대신 여기서 전체 태그를
  뿌리지 않는다.
- 결과는 이름순 정렬, **최대 20건**. 자동완성 한 화면에 들어갈 만큼만 내려간다(페이지네이션 없음).
- `%`, `_` 같은 LIKE 와일드카드는 **문자 그대로** 검색된다. `q=%`는 전체가 아니라 이름에 `%`가
  들어간 태그만 찾는다.

### 태그 이름 규칙
발행(`submit`)과 수정(`PATCH`)이 **완전히 같은 규칙**을 쓴다.

- 보내는 것은 **이름 배열**이다: `{ "tags": ["오운완", "일상"] }`. `tagId`가 아니다.
- 없는 이름이면 **자동 생성**된다. 태그를 미리 만들어둘 필요가 없다.
- **대소문자를 무시해 같은 태그로 묶인다.** 이미 `Cat`이 있는데 `cat`을 보내면 새 태그가
  생기지 않고 기존 `Cat`에 연결된다(응답에도 저장된 철자인 `Cat`이 내려온다). 이 판정이
  없으면 사실상 같은 태그가 철자만 다른 행으로 늘어난다.
- 앞뒤 공백은 제거된다. `"  일상  "`과 `"일상"`은 같은 태그다.
- 빈 문자열·공백뿐인 항목과 한 요청 안의 중복은 **조용히 버려진다**. 입력창을 쉼표로 쪼개면
  빈 칸이 딸려오기 쉬운데 그것 때문에 발행이 막히면 원인을 알기 어렵기 때문이다.
- **50자 초과 → 400**, **일기당 11개 이상 → 400**. 이쪽은 조용히 자르지 않는다 — 사용자가
  실제로 입력한 내용이 사라지는 것이라 알려주는 편이 맞다.

```json
// 요청
{ "tags": ["  오운완  ", "오운완", "", "Cat", "cat"] }
// 실제로 달리는 태그 (공백 제거 · 중복 제거 · 기존 Cat 재사용)
[{ "tagId": 1, "name": "Cat" }, { "tagId": 5, "name": "오운완" }]
```

### 태그와 일기의 수명
- 일기를 삭제하면 그 일기의 `diary_tags` 연결은 **함께 삭제**된다([일기 삭제](#일기-삭제) 참고).
- 그러나 **`tags` 사전의 행은 남는다.** 다른 일기가 같은 태그를 쓰고 있을 수 있고, 아무도 쓰지
  않는 태그가 남아도 검색 결과에 뜰 뿐 문제가 되지 않는다. 즉 "검색에는 나오는데 그 태그가
  달린 일기는 0건"인 상태가 정상적으로 존재할 수 있다.

> **왜 `POST/DELETE /api/diaries/{id}/tags` 같은 전용 API가 없나**
>
> 태그는 댓글·좋아요처럼 사용자가 남기는 별개의 상호작용이 아니라 제목·공개범위·카테고리와
> 같은 **일기의 속성**이다. 그래서 일기를 만드는 자리(발행)와 일기를 고치는 자리(수정)에
> 얹었다. 전용 엔드포인트를 따로 두면 "발행 직후 태그를 달려면 두 번 호출"이 되고, 태그만
> 부분 추가·삭제하는 별도 규칙이 생겨 수정 API와 의미가 갈린다.
>
> 대신 `PATCH`의 `tags`는 **부분 추가가 아니라 교체**다. 태그 하나를 떼려면 남길 목록 전체를
> 보내야 하는데, 일기당 10개 상한이라 클라이언트가 현재 목록을 들고 있으면 그대로 보내면 된다.

## Category
일기를 분류하는 사용자별 태그. 카테고리는 **만든 사람에게만 보이고**, 이름은 사용자 안에서만 유일하다(`UNIQUE(user_id, name)`) — 다른 사용자가 같은 이름을 쓰는 것은 자유롭다.

일기에 카테고리를 붙이는 시점은 발행(`POST /api/rooms/{roomId}/submit`)이며, 그때 `categoryId`를 함께 보낸다.

### 내 카테고리 목록
- GET /api/users/me/categories
- Response 200: [{ categoryId, name }]
```json
[
  { "categoryId": 4, "name": "맛집 탐방" },
  { "categoryId": 5, "name": "여행" }
]
```
- 생성한 순서(오래된 것부터)로 내려감. 없으면 빈 배열 `[]`.

### 카테고리 생성
- POST /api/users/me/categories
- Body: { name }
- Response **201**: { categoryId, name }
- 400: `name`이 비었거나 공백뿐이거나 50자 초과
- 409: 같은 이름의 카테고리를 **내가** 이미 갖고 있는 경우 `{ "message": "이미 존재하는 카테고리입니다: 여행" }`
  - 다른 사용자가 같은 이름을 쓰고 있는 것은 충돌이 아님.

### 카테고리 수정
- PATCH /api/users/me/categories/{categoryId}
- Body: { name } — **필수**. 바꿀 것이 이름뿐이라 부분 수정을 지원하지 않음.
- Response 200: { categoryId, name }
- 400: `name`이 비었거나 공백뿐이거나 50자 초과
- 403: 남의 카테고리인 경우 `{ "message": "본인의 카테고리가 아닙니다: 4" }`
- 404: 카테고리가 없는 경우
- 409: 내 다른 카테고리와 이름이 겹치는 경우. 단 **이름을 그대로 보내면 200**(자기 자신과는 충돌하지 않음).

### 카테고리 삭제
- DELETE /api/users/me/categories/{categoryId}
- Response 200: { "message": "카테고리가 삭제되었습니다" }
- 403: 남의 카테고리인 경우
- 404: 카테고리가 없는 경우
- ⚠️ **이 카테고리를 쓰던 일기는 지워지지 않는다.** 해당 일기들의 `category_id`가 null로 바뀔 뿐이며, 이후 그 일기를 조회하면 `categoryId`/`categoryName`이 둘 다 null로 내려간다. 분류는 일기에 붙은 꼬리표일 뿐이라 꼬리표를 떼는 일이 일기를 지우는 결과가 되어서는 안 되기 때문.

## Realtime
### 실시간 협업 연결
- WebSocket /ws/diaries/{diaryId}

### 그림 객체 생성
- SEND /app/diaries/{diaryId}/objects/create
- Body: { objectId, objectType, objectData }

### 그림 객체 수정
- SEND /app/diaries/{diaryId}/objects/update
- Body: { objectId, objectData }

### 그림 객체 삭제
- SEND /app/diaries/{diaryId}/objects/delete
- Body: { objectId }

### 변경사항 구독
- SUBSCRIBE /topic/diaries/{diaryId}
- Response: { eventType, userId, objectId, objectData, timestamp }

## Friend
### 친구 요청
- POST /api/friends/requests
- Body: { receiverId }
- Response: { requestId, status }

### 친구 요청 목록 조회
- GET /api/friends/requests
- Response: [{ requestId, senderId, senderNickname, status }]

### 친구 요청 수락
- POST /api/friends/requests/{requestId}/accept
- Response: { requestId, status }

### 친구 요청 거절
- POST /api/friends/requests/{requestId}/reject
- Response: { requestId, status }

### 친구 목록 조회
- GET /api/friends
- Response: [{ userId, nickname, profileImageUrl }]

### 친구 삭제
- DELETE /api/friends/{userId}
- Response: { message }

## Like
좋아요는 **일기를 볼 수 있는 사람만** 누를 수 있음. 판정 기준은 [일기 상세 조회](#일기-상세-조회)의 공개 범위 표와 동일 — 일기가 403이면 좋아요도 403.

### 좋아요 등록
- POST /api/diaries/{diaryId}/likes
- Body 없음
- Response 200: { diaryId, liked, likeCount }
  - `liked`는 요청 후 상태라 항상 `true`. `likeCount`는 해당 일기의 **전체** 좋아요 수(내 것 포함).
- 403: 일기 조회 권한이 없는 경우
- 404: 일기가 없거나 이미 삭제된 경우
- **멱등**. 이미 눌러둔 상태에서 다시 호출해도 409가 아니라 200이고 `likeCount`도 그대로. 버튼 연타로 요청이 겹쳐도 중복 행이 생기지 않음(`UNIQUE(diary_id, user_id)`).
- 201이 아니라 **200**임에 주의(댓글 작성만 201).

### 좋아요 취소
- DELETE /api/diaries/{diaryId}/likes
- Response 200: { diaryId, liked, likeCount }
  - `liked`는 항상 `false`.
- **멱등**. 누른 적 없는 좋아요를 취소해도 200 — 클라이언트는 응답과 무관하게 UI를 "취소됨"으로 두면 됨.
- 등록과 달리 **조회 권한을 보지 않음**. 이미 남긴 좋아요는 나중에 일기가 비공개로 바뀌어도 취소할 수 있어야 하기 때문. 단 일기 자체가 없으면 404.
- 취소 후 다시 등록 가능.

### 좋아요 누른 사람 목록
- GET /api/diaries/{diaryId}/likes
- 인증 필요
- Response 200: `[{ userId, nickname, profileImageUrl }]`
- **최신순**(마지막에 누른 사람이 앞). 좋아요는 취소·재등록이 가능해서 "누른 순서"는 마지막 등록 시점 기준이다
- 403: 일기 조회 권한이 없는 경우 — 못 보는 일기는 누가 눌렀는지도 볼 수 없다
- 404: 일기가 없거나 이미 삭제된 경우
- 아무도 안 눌렀으면 빈 배열 `[]`
- **탈퇴한 계정은 목록에서 빠진다.** 그래서 이 배열의 길이가 좋아요 등록·취소 응답의 `likeCount`보다 **작을 수 있다.** 일부러 다르게 뒀다 — `likeCount`는 누른 기록 그대로여야 랭킹 점수가 흔들리지 않고, 목록은 지금 남아 있는 사람만 보여주는 게 맞다

### 랭킹과의 관계
좋아요 수가 랭킹 점수에 들어가므로, 좋아요가 **실제로** 등록·취소될 때 그 일기의
`likeScore`와 `totalScore`가 즉시 다시 계산된다([점수 공식](#점수-공식-팀-확정)).
멱등 재호출은 변한 게 없어 재계산하지 않고, 아직 AI 점수가 없는 일기는 그대로 둔다.

## Comment
댓글도 **일기를 볼 수 있는 사람만** 조회·작성 가능(공개 범위 판정은 일기 상세 조회와 동일). 못 보는 일기의 댓글은 목록조차 열리지 않음.

### 댓글 목록 조회
- GET /api/diaries/{diaryId}/comments
- Response 200: [{ id, userId, nickname, content, createdAt }]
- **오래된 순**(작성 시각 오름차순) 정렬. 댓글이 없으면 빈 배열 `[]`.
- 403: 일기 조회 권한이 없는 경우
- 404: 일기가 없거나 이미 삭제된 경우
- 작성자가 탈퇴한 댓글은 목록에서 빠짐(일기 목록의 `authorNickname` 처리와 같은 규칙). 그래서 목록 길이가 실제 댓글 수와 다를 수 있음.

### 댓글 작성
- POST /api/diaries/{diaryId}/comments
- Body: { content } — 필수
- Response **201**: { id, content, createdAt }
  - 목록 응답과 필드가 다름(`userId`/`nickname` 없음). 작성 직후 화면에 붙일 때는 요청자 본인 정보를 쓰면 됨.
- 400: `content`가 없거나 빈 문자열/공백뿐이거나 1000자 초과
- 403: 일기 조회 권한이 없는 경우 — **댓글도 못 보는 일기에는 댓글을 달 수 없음**
- 404: 일기가 없거나 이미 삭제된 경우
- 협업자가 아니어도 됨. `PUBLIC` 일기라면 로그인한 누구나 작성 가능.

### 댓글 수정
- PATCH /api/comments/{commentId}
- 일기 경로가 아니라 `commentId`만으로 호출함에 주의(삭제와 같다).
- Body: { content } — 필수
- Response 200: { id, content, updatedAt }
- 400: `content`가 없거나 빈 문자열/공백뿐이거나 1000자 초과 — **작성과 완전히 같은 제한**
- 403: **작성자 본인이 아닌 경우.** 일기 협업자나 방장이라도 남의 댓글 내용은 바꿀 수 없다
- 404: 댓글이 없거나 이미 삭제된 경우
- 삭제와 마찬가지로 **일기 조회 권한을 다시 보지 않는다.** 이미 남긴 내 글을 고치는 일이라, 나중에 일기가 비공개로 바뀌었다고 손댈 수 없게 되면 곤란하기 때문
- **알림이 가지 않는다.** 수정할 때마다 알림이 다시 가면 댓글 하나로 알림을 몇 번이든 만들 수 있다(작성 시에만 `COMMENT` 알림)

### 댓글 삭제
- DELETE /api/comments/{commentId}
- 일기 경로가 아니라 `commentId`만으로 호출함에 주의.
- Response 200: { "message": "댓글이 삭제되었습니다" }
- 403: **작성자 본인이 아닌 경우**. 일기 협업자나 방장이라도 남의 댓글은 지울 수 없음.
- 404: 댓글이 없거나 이미 삭제된 경우
- 하드 삭제라 복구 불가.

### 일기 삭제와의 관계
`DELETE /api/diaries/{diaryId}`로 일기를 지우면 그 일기의 댓글과 좋아요도 **함께 영구 삭제**됨. 이후 해당 일기의 댓글·좋아요 API는 모두 404.

## Notification
알림은 **직접 만드는 API가 없음**. 아래 네 이벤트가 일어날 때 서버가 자동으로 남긴다.

| 이벤트 | type | 받는 사람 | 보낸 사람 | targetId |
|---|---|---|---|---|
| 방 초대 | `ROOM_INVITE` | 초대받은 사람 | 초대한 사람 | roomId |
| 팔로우 | `FOLLOW` | 팔로우당한 사람 | 팔로우한 사람 | **null** |
| 댓글 작성 | `COMMENT` | 일기 작성자(방장) | 댓글 작성자 | diaryId |
| 좋아요 등록 | `LIKE` | 일기 작성자(방장) | 좋아요 누른 사람 | diaryId |

- `targetId`는 type에 따라 가리키는 대상이 다름(roomId / diaryId / 없음). FK가 아니므로 **대상이 삭제돼도 값은 그대로 남음** — 알림을 눌러 이동한 화면이 404일 수 있으니 클라이언트에서 처리 필요.
- **자기 자신에게는 알림이 생기지 않음.** 본인 일기에 본인이 댓글/좋아요를 달거나, 본인을 방에 초대해도 알림 없음(동작 자체는 정상 수행됨).
- 알림 생성은 원래 동작과 **같은 트랜잭션**. 팔로우가 롤백되면 알림도 함께 사라지고, 반대로 알림이 없다고 원래 동작이 실패하지는 않음.
- 협업자가 모두 탈퇴해 일기 작성자를 특정할 수 없으면 댓글/좋아요 알림은 생기지 않음(댓글·좋아요 자체는 정상 동작).

**중복 방지 규칙** — 멱등한 API가 알림만 쌓지 않도록:
- 이미 멤버이거나 PENDING 초대가 살아 있는 사람을 **재초대**해도 알림이 다시 생기지 않음.
- **중복 좋아요**(이미 누른 상태에서 재호출)는 알림이 생기지 않음. **좋아요 취소도 알림 없음.** 단 취소 후 다시 누르면 새 알림이 감.
- 이미 팔로우 중이면 409라 알림이 생기지 않음. 언팔로우 후 재팔로우는 새 관계라 알림이 다시 감.
- 댓글은 같은 사람이 여러 번 달면 **그때마다** 알림이 감(댓글 자체가 매번 새로 생기므로).

### 내 알림 목록
- GET /api/notifications
- Response 200: [{ notificationId, senderId, senderNickname, type, targetId, isRead, createdAt }]
- 요청자가 **받은** 알림만. 최신순(notificationId 내림차순). 없으면 빈 배열 `[]`.
- 페이지네이션 없음.
- `senderId`/`senderNickname`이 **둘 다 null**인 경우:
  - 시스템 알림(현재 이 알림을 만드는 경로는 없지만 형식상 가능)
  - **보낸 사람이 탈퇴한 경우** — 알림 자체는 목록에 남고 sender 정보만 null이 됨. `type`·`targetId`·`createdAt`은 그대로라 "누군가 회원님을 팔로우했습니다" 식으로 표시하면 됨.
- `isRead`는 boolean. JSON 키가 `read`가 아니라 **`isRead`**임에 주의.

### 읽음 처리
- PATCH /api/notifications/{notificationId}/read
- Body 없음
- Response 200 (**본문 없음**)
- 403: 본인에게 온 알림이 아닌 경우
- 404: 알림이 없는 경우
- **멱등**. 이미 읽은 알림을 다시 호출해도 200.

### 전체 읽음 처리
- PATCH /api/notifications/read-all
- Body 없음
- Response 200: { updatedCount }
- `updatedCount`는 이번 호출로 **실제로 바뀐 건수**(= 안 읽은 알림 수). 이미 다 읽었으면 `0`이고 에러가 아님.
- 요청자 본인의 알림만 대상. 받은 알림이 하나도 없어도 200 / `0`.

### ⚠️ DB 스키마 변경 필요
`notifications.sender_id`와 `target_id`가 **NOT NULL이면 안 됨**(FOLLOW 알림은 `target_id`가 null, 시스템/탈퇴 알림은 `sender_id`가 null). `ddl-auto: update`는 이미 있는 컬럼의 NOT NULL을 풀어주지 않으므로, 기존에 이 테이블이 만들어져 있던 환경은 아래를 한 번 실행해야 함:

```sql
ALTER TABLE notifications
    ALTER COLUMN sender_id DROP NOT NULL,
    ALTER COLUMN target_id DROP NOT NULL;
```

테이블을 처음부터 새로 만드는 환경(볼륨을 비운 경우 등)은 엔티티 정의대로 생성되므로 실행할 필요 없음.

## AI
AI 서버(FastAPI)는 별도 배포되어 있고, 백엔드가 대신 호출한다. **프론트는 AI 서버에 직접
붙지 않는다** — 인증·이미지 저장·점수 계산이 전부 백엔드에 있기 때문이다.

### AI 서버 설정
| 설정 | 환경변수 | 기본값 |
|---|---|---|
| 주소 | `AI_SERVER_URL` | `https://drawing-diary.onrender.com` |
| connect timeout | — | 10s |
| read timeout | — | 120s |

로컬에서 AI 서버를 띄웠다면 `AI_SERVER_URL=http://localhost:8000`으로 바꿔 붙는다.

> **read timeout이 120초인 이유**: Render 무료 티어는 일정 시간 요청이 없으면 잠들고,
> 깨어나는 데만 50초 안팎이 걸린다(실측 52초). 여기에 생성 시간 10~13초가 더 붙으므로
> 콜드 스타트 최악이 약 65초다. 짧게 잡으면 첫 사용자가 항상 타임아웃을 본다.

### AI 서버 호출 실패 시
모두 **502 Bad Gateway**로 내려가고, 메시지로 원인이 구분된다. 우리 서버 잘못이 아니라
뒤에 있는 AI 서버가 응답을 못 준 것이라 500이 아니다.

| 메시지 | 원인 | 프론트 대응 |
|---|---|---|
| AI 서버 응답이 시간 안에 오지 않았습니다 | read timeout. 깨어나는 중일 수 있음 | 잠시 후 재시도 안내 |
| AI 서버에 연결할 수 없습니다 | 연결 거부·DNS 실패·5xx | 잠시 후 재시도 안내 |
| AI 서버가 요청을 거부했습니다 | 4xx. 요청이 AI 스펙과 안 맞음 | 재시도해도 소용없음, 문의 |
| AI 서버 응답 형식이 올바르지 않습니다 | 200인데 본문이 기대한 형식이 아님 | 재시도해도 소용없음, 문의 |

### 선화 가이드 생성
- POST /api/rooms/{roomId}/ai-guide
- 인증 필요
- Body: { text }
  - `text` 필수(공백만이면 400), 5000자 이하
- Response 200:
```json
{
  "guides": [
    { "style": "1", "styleName": "웹툰형",               "imageUrl": "https://.../api/images/9",  "error": null },
    { "style": "2", "styleName": "컬러링북형",           "imageUrl": "https://.../api/images/10", "error": null },
    { "style": "3", "styleName": "고퀄리티 애니메이션형", "imageUrl": "https://.../api/images/11", "error": null }
  ]
}
```
  - `imageUrl`이 가리키는 이미지는 **JPEG**로 저장된다(AI 서버가 주는 형식 그대로).
    업로드 허용 형식(png/jpeg/webp)과는 별개이며, 프론트는 확장자를 가정하지 말고 URL을 그대로 쓰면 된다.
- 400: `text` 누락·공백·5000자 초과
- 403: 방 멤버가 아닌 경우
- 404: 방이 없거나 삭제된 경우
- 409: 이미 `FINISHED`인 방 (발행이 끝난 방에는 가이드를 만들지 않는다)
- 502: **3종 전부** 실패

#### 부분 실패
3종 중 일부만 실패해도 **200**이다. 실패한 스타일은 목록에서 빠지지 않고 `imageUrl: null` +
`error`에 사유가 담겨 내려온다. 프론트가 성공한 그림을 먼저 보여주고 실패한 자리에만
재시도 버튼을 그릴 수 있게 하기 위해서다.

```json
{ "style": "2", "styleName": "컬러링북형", "imageUrl": null, "error": "AI 서버 응답이 시간 안에 오지 않았습니다 (style=2)" }
```

전부 실패했을 때만 502가 나간다(보여줄 게 하나도 없으므로).

#### 성능
세 스타일을 **병렬로** 호출한다. 실측으로 순차 호출은 약 35초, 병렬은 **약 12.6초**다.
서버가 잠들어 있었다면 여기에 기상 시간(약 50초)이 한 번 더 붙는다.

#### 저장 형식 주의
AI 서버는 응답 헤더에 `Content-Type: image/png`를 붙이지만 **실제 바이트는 JPEG**다.
백엔드는 헤더를 믿지 않고 매직 넘버로 형식을 판별해서 저장하므로, `/api/images/{id}`는
`image/jpeg`로 서빙된다. (헤더를 그대로 믿으면 `X-Content-Type-Options: nosniff` 때문에
브라우저가 JPEG를 PNG로 디코딩하려다 실패해 그림이 깨진다.)

### AI 점수 저장
- POST /api/diaries/{diaryId}/scores
- 인증 필요
- Body: `{ relevanceScore, colorScore }` — 둘 다 필수, 각각 0~100 정수
  - 좋아요 점수와 총점은 **백엔드가 계산**하므로 요청에 넣어도 무시된다(필드 자체가 없다)
- Response 200: `{ diaryId, relevanceScore, colorScore, likeScore, totalScore, feedback }`
```json
{
  "diaryId": 83,
  "relevanceScore": 95,
  "colorScore": 92,
  "likeScore": 0,
  "totalScore": 75,
  "feedback": "일기에 기술된 부드러운 그라데이션 배경이 그림에 매우 잘 표현되어 있습니다."
}
```
- 재호출하면 **갱신**이다(일기와 1:1). 새 자원이 생기는 게 아니라서 201이 아니라 200.
- 400: 점수 누락 또는 0~100 범위를 벗어난 값
- 403: 그 일기를 볼 수 없는 경우(조회 권한과 같은 판정)
- 404: 없는 diaryId
- ⚠️ **`feedback`은 이 요청으로 저장할 수 없다.** Body에 코멘트 필드가 없고, 수동 저장은
  **기존 `ai_comment`를 그대로 유지**한다 — 점수를 다시 매겼다고 AI가 남긴 코멘트를 지울
  이유가 없기 때문이다. 아직 AI 산정을 돌린 적이 없으면 `feedback`은 null로 내려온다.

### AI 점수 조회
- GET /api/diaries/{diaryId}/scores
- 인증 필요
- Response 200: 저장과 **같은 형식**(`feedback` 포함)
- 403: 그 일기를 볼 수 없는 경우
- 404: 없는 diaryId, 또는 아직 점수가 저장되지 않은 일기
- `likeScore`는 저장된 값이 아니라 **조회 시점의 좋아요 수**로 계산해서 내려간다.

#### `feedback` 필드
- 출처는 `ai_scores.ai_comment` — [AI 점수 자동 산정](#ai-점수-자동-산정)이 AI 서버에서 함께 받아 저장한 평가 코멘트다.
- **점수 계산에는 들어가지 않는다.** 랭킹·총점과 무관한 표시용 기록이다.
- 컬럼이 NULL이든 빈 문자열이든 응답에서는 **항상 null**로 맞춰 내려간다. 프론트는 `feedback == null` 한 가지만 보고 "코멘트 없음"을 판정하면 된다.
- 세 경로(`POST .../scores`, `GET .../scores`, `POST .../ai-score`)의 응답 형식이 같으므로 같은 코드로 처리하면 된다.

### AI 점수 자동 산정
- POST /api/diaries/{diaryId}/ai-score
- 인증 필요, **Body 없음**
- Response 200: `{ diaryId, relevanceScore, colorScore, likeScore, totalScore, feedback }`
  — [AI 점수 저장](#ai-점수-저장)과 **완전히 같은 형식**이라 프론트는 두 경로를 같은 코드로 처리하면 된다
  - `feedback`은 이번 호출에서 AI 서버가 준 평가 코멘트다. AI가 코멘트를 비워 보내면 직전 값이 유지되고, 그것도 없으면 null
- 400: 일기에 `final_img_url`이 없는 경우(평가할 이미지가 없음)
- 403: 해당 일기의 **협업자가 아닌** 경우 — 점수는 일기에 남는 기록이라 볼 수만 있는 사람은 매길 수 없다
- 404: 없는 diaryId
- 502: AI 서버 호출 실패

동작:
1. 일기에서 `content`와 `final_img_url`을 읽는다
2. `final_img_url`이 `/api/images/{id}` 형태면 **DB에서 바이너리를 바로 꺼내고**(자기 자신에게
   HTTP 요청을 보내지 않는다), 외부 URL이면 다운로드한다(10MB 상한, png/jpeg/webp만)
3. AI 서버 `POST /api/ai/score`에 multipart(`text` + `image`)로 보낸다
4. 받은 점수와 `feedback`을 [AI 점수 저장](#ai-점수-저장)과 **같은 로직·한 트랜잭션**으로
   저장한다 — `likeScore`·`totalScore` 계산이 한 곳에만 있어 수동 저장과 어긋나지 않고,
   점수만 저장되고 코멘트가 빠지는 중간 상태도 생기지 않는다

이미 점수가 있으면 갱신된다(일기와 1:1). 좋아요 점수는 저장 시점의 좋아요 수로 다시 계산된다.

### AI 서버 실제 응답 형식 (확인 완료)
AI 서버의 `/openapi.json`과 실호출로 확인한 결과다.

**POST /api/ai/guide** — `application/json` 요청
```json
{ "text": "일기 내용", "style": "1" }
```
응답은 이미지 바이너리(위 "저장 형식 주의" 참고).

**POST /api/ai/score** — `multipart/form-data` 요청, 파트는 `text`와 `image` **두 개뿐**
```json
{ "relevanceScore": 95, "colorScore": 5, "feedback": "일기에 언급된 공원, 벤치, 노을이 그림에 잘 표현되어..." }
```

정리:
- ✅ `relevanceScore`·`colorScore`는 예상대로 내려온다
- ⚠️ **`feedback`(평가 코멘트) 필드가 하나 더 있다.** OpenAPI 스펙상 필수(required) 세 개 중 하나다
- ✅ **`like_count` 같은 추가 파라미터는 요구하지 않는다.** 요청 파트는 `text`·`image`뿐이고,
  응답에도 좋아요 관련 값이 없다 — 좋아요 점수와 총점 계산은 백엔드 몫이 맞다

`feedback`은 `ai_scores.ai_comment` 컬럼에 저장되고, **이제 세 경로의 응답에 모두 포함된다**
(`POST .../scores`, `GET .../scores`, `POST .../ai-score`). 수동 저장 경로와 형식을 똑같이
유지하는 계약은 그대로다 — 세 응답이 함께 필드를 얻었기 때문이다. 자세한 동작은
[`feedback` 필드](#feedback-필드) 참고.

## Ranking
`ai_scores`가 있는 **PUBLIC** 일기만 대상. 비공개 일기는 점수가 있어도 랭킹에 나오지 않는다.

정렬은 `totalScore` 내림차순, **동점이면 diaryId 오름차순**. 두 키를 합치면 전순서라
같은 데이터에 대해 순위가 요청마다 달라지지 않는다.

> ### ⚠️ `rank`의 의미가 엔드포인트마다 다르다
>
> | 엔드포인트 | `rank`의 뜻 |
> |---|---|
> | `GET /api/rankings` | **그 목록에서 몇 번째** (`rank = offset + 순번`) |
> | `GET /api/rankings/friends` | **그 목록에서 몇 번째** (`rank = offset + 순번`) |
> | `GET /api/rankings/me` | **전체 랭킹에서 몇 위** |
>
> 친구 랭킹은 목록 안 순위다. 전체 순위로 매기면 화면에 `37위·102위·415위`처럼 찍혀
> 리더보드로 읽히지 않고, `offset`을 넘길 때 `rank`가 건너뛰어 "몇 번째 항목인지"조차
> 알 수 없게 된다. 반대로 내 랭킹은 "전체에서 어디쯤인가"가 곧 질문이라 전역 순위여야 한다.
> 두 화면에서 같은 일기의 `rank`가 다르게 보일 수 있는데, 버그가 아니다.

### 전체 랭킹
- GET /api/rankings?offset=0&limit=20
- 인증 필요
- `limit` 기본 20, 최대 50 (벗어나면 조용히 보정). `offset` 기본 0
- Response 200: `[{ rank, diaryId, title, thumbnailUrl, totalScore, authorId, authorNickname }]`
  - `rank`는 1부터. `offset=20`이면 21위부터 내려온다
  - `thumbnailUrl`은 `diaries.final_img_url` — `POST /api/images`로 올린
    `/api/images/{id}` 형태의 URL이 그대로 들어갈 수 있다
  - `authorId`·`authorNickname`은 피드·목록과 같은 기준(첫 생존 협업자)
- 범위를 벗어난 `offset`이면 빈 배열 `[]`

> **왜 `cursor`가 아니라 `offset`인가**
> 피드·탐색은 정렬 키가 `diaryId` 하나뿐이라 "이 id보다 작은 것"으로 자를 수 있다. 랭킹은
> 정렬 키가 `(totalScore, diaryId)` 두 개라 `diaryId` 커서로는 페이지 경계가 맞지 않고,
> 무엇보다 화면에 찍을 `rank`가 **몇 번째부터인지**를 알아야 나온다. `offset`이면
> `rank = offset + 순번`으로 정확히 떨어진다.
>
> 대신 스크롤 도중 점수가 바뀌면 항목이 밀려 중복·누락이 생길 수 있다. 랭킹은 AI 점수 저장과
> 좋아요 변동에만 움직여 피드만큼 자주 변하지 않아 감수할 만하다고 봤다. 실시간성이 중요해지면
> `(totalScore, diaryId)` 복합 커서로 바꾸되, 그때는 `rank`를 클라이언트가 누적 계산해야 한다.

### 친구 랭킹
- GET /api/rankings/friends?offset=0&limit=20
- 인증 필요
- **내가 팔로우하는 사람이 협업자로 참여한** 일기 중 AI 점수가 있는 것
- `limit` 기본 20, 최대 50 (벗어나면 조용히 보정). `offset` 기본 0
- Response 200: `[{ rank, diaryId, title, thumbnailUrl, totalScore, authorId, authorNickname }]` — [전체 랭킹](#전체-랭킹)과 **같은 형식**
- 정렬·대상(PUBLIC만)·페이지네이션 모두 전체 랭킹과 같고, 대상 집합만 좁다
- 팔로우하는 사람이 없거나 그들의 일기에 점수가 없으면 빈 배열 `[]`
- "협업자 중 누구라도"가 기준이라, 방장이 아니어도 팔로우한 사람이 같이 그린 일기면 포함된다(팔로잉 피드와 같은 판단). 한 일기에서 여러 명을 팔로우 중이어도 **한 번만** 나온다

> **내가 쓴 일기도 나올 수 있다**
> 자기 자신은 팔로우할 수 없으므로 **혼자 그린 일기는 나오지 않는다.** 하지만 내가 팔로우하는
> 사람과 **같이 그린** 일기는 그 사람이 협업자라서 포함되고, 이때 `authorNickname`은 나로
> 표시된다(작성자 = 첫 협업자). 버그가 아니라 위의 "협업자 중 누구라도" 규칙의 결과다.
> 순수하게 내 순위만 보려면 [내 랭킹](#내-랭킹)을 쓴다.

### 내 랭킹
- GET /api/rankings/me
- 인증 필요
- 내가 협업자인 일기 중 점수가 있는 것들의 **전체 랭킹 기준 순위**
- Response 200: `[{ diaryId, rank, totalScore }]` — `rank` 오름차순. 해당 없으면 `[]`
- 페이지네이션 없음(보통 몇 건 안 됨)
- 전체 랭킹이 PUBLIC만 대상이라 여기서도 PUBLIC만 나온다. 비공개 일기는 공개 랭킹에 자리를
  가질 수 없으므로 점수가 있어도 이 목록에 없다.

## Image
[이미지 업로드](#이미지-업로드)·[이미지 조회](#이미지-조회)는 Room 아래에 정리되어 있다.
