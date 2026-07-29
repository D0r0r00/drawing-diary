# drawing-diary-backend

그림 일기 서비스 백엔드 (Spring Boot)

## 담당
최민석

## 기술 스택
- Java 17
- Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL

## 시작하기
```bash
# 1. infra 폴더에서 DB 먼저 실행
cd infra && docker-compose up -d

# 2. 루트로 돌아와서 Spring Boot 실행
cd .. && ./gradlew bootRun
```
→ http://localhost:8080

## API 목록
| Method | URL | 기능 |
|--------|-----|------|
| POST | /api/auth/signup | 회원가입 |
| POST | /api/auth/login | 로그인 (JWT 발급) |
| GET | /api/diaries | 내 일기 목록 |
| POST | /api/diaries | 일기 생성 |
| GET | /api/diaries/{id} | 일기 상세 |
| GET | /api/ranking | 랭킹 조회 |
