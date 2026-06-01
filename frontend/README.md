# Frontend

## 프로젝트 초기화 (처음 한 번만)
```bash
npx create-next-app@latest . --typescript --tailwind --app
npm install axios
```

## 개발 서버 실행
```bash
npm run dev
```
→ http://localhost:3000

## 주요 페이지
- `/` : 메인 홈 (일기 목록)
- `/login` : 로그인
- `/signup` : 회원가입
- `/diary/[id]` : 일기 + 캔버스 편집
- `/profile/[id]` : 프로필
- `/ranking` : 랭킹
