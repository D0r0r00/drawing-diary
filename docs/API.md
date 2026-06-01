# API 명세서

## 인증
모든 API (로그인/회원가입 제외)는 Header에 JWT 포함 필요
```
Authorization: Bearer {토큰}
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

## Diary
### 일기 목록 조회
- GET /api/diaries
- Response: [{ id, title, createdAt, thumbnailUrl }]

### 일기 생성
- POST /api/diaries
- Body: { title, textContent, visibility }
- Response: { id, title, createdAt }

## AI
### 키워드 추출
- POST /ai/extract-keywords (AI 서버 직접)
- Body: { text }
- Response: { keywords: [] }

### 선화 가이드 생성
- POST /ai/generate-guide
- Body: { text }
- Response: { image_base64, prompt_used }

### 점수 산정
- POST /ai/score-diary
- Body: { text, color_count }
- Response: { total_score, emotion_score, completion_score, color_score }
