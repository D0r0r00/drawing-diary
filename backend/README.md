# Backend

## 프로젝트 초기화
start.spring.io 에서 생성:
- Project: Gradle - Kotlin
- Spring Boot: 3.x
- Dependencies: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Lombok

생성된 파일을 이 폴더에 압축 해제 후 GitHub push

## 실행 방법
```bash
./gradlew bootRun
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
