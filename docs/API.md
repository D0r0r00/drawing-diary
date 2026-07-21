# API 명세서

## 인증
로그인과 회원가입을 제외한 백엔드 API는 Header에 JWT 포함 필요
```
Authorization: Bearer {accessToken}
실시간 협업 연결 시에도 JWT 인증 필요
```

## Auth
### 회원가입
- POST /api/auth/signup
- Body: { email, password, nickname }
- Response: { userId, email, nickname }

### 로그인
- POST /api/auth/login
- Body: { email, password }
- Response: { accessToken, userId }

## User
### 내 정보 조회
- GET /api/users/me
- Response: { id, email, nickname, profileImageUrl }

### 프로필 수정
- PATCH /api/users/me
- Body: { nickname, profileImageUrl }
- Response: { id, nickname, profileImageUrl }

### 사용자 검색
- GET /api/users/search?keyword={검색어}
- Response: [{ id, nickname, profileImageUrl }]

## Diary
### 일기 목록 조회
- GET /api/diaries
- Response: [{ id, title, authorId, authorNickname, createdAt, thumbnailUrl, visibility }]

### 내 일기 목록 조회
- GET /api/diaries/my
- Response: [{ id, title, createdAt, thumbnailUrl, visibility }]

### 일기 상세 조회
- GET /api/diaries/{diaryId}
- Response: { id, title, textContent, canvasData, imageUrl, visibility, createdAt }

### 일기 생성
- POST /api/diaries
- Body: { title, textContent, visibility }
- Response: { id, title, createdAt }

### 일기 수정
- PATCH /api/diaries/{diaryId}
- Body: { title, textContent, visibility }
- Response: { id, title, updatedAt }

### 일기 삭제
- DELETE /api/diaries/{diaryId}
- Response: { message }

### 캔버스 저장
- PUT /api/diaries/{diaryId}/canvas
- Body: { canvasData, imageUrl }
- Response: { diaryId, savedAt }

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
### 좋아요 등록
- POST /api/diaries/{diaryId}/likes
- Response: { diaryId, liked, likeCount }

### 좋아요 취소
- DELETE /api/diaries/{diaryId}/likes
- Response: { diaryId, liked, likeCount }

## Comment
### 댓글 목록 조회
- GET /api/diaries/{diaryId}/comments
- Response: [{ id, userId, nickname, content, createdAt }]

### 댓글 작성
- POST /api/diaries/{diaryId}/comments
- Body: { content }
- Response: { id, content, createdAt }

### 댓글 삭제
- DELETE /api/comments/{commentId}
- Response: { message }

## AI
### 키워드 추출
- POST /ai/extract-keywords 
- Body: { text }
- Response: { keywords: [] }

### 선화 가이드 생성
- POST /ai/generate-guide
- Body: { text, keywords }
- Response: { imageBase64, promptUsed }

### 일기 점수 산정
- POST /ai/score-diary
- Body: { diaryId, text, imageBase64, colorCount }
- Response: { totalScore, themeScore, relationScore, colorScore, feedback }

### AI 점수 저장
- POST /api/diaries/{diaryId}/scores
- Body: { totalScore, themeScore, relationScore, colorScore, feedback }
- Response: { scoreId, diaryId, totalScore, createdAt }

### AI 점수 조회
- GET /api/diaries/{diaryId}/scores
- Response: { diaryId, totalScore, themeScore, relationScore, colorScore, feedback }

## Ranking
### 전체 랭킹 조회
- GET /api/rankings?period={DAILY|WEEKLY}
- Response: [{ rank, userId, nickname, profileImageUrl, score }]

### 친구 랭킹 조회
- GET /api/rankings/friends?period={DAILY|WEEKLY}
- Response: [{ rank, userId, nickname, score }]

### 내 랭킹 조회
- GET /api/rankings/me?period={DAILY|WEEKLY}
- Response: { rank, userId, nickname, score }

## Image
### 이미지 업로드
- POST /api/images
- Content-Type: multipart/form-data
- Body: { file }
- Response: { imageUrl }