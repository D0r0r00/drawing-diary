# Infra 

## 실행 방법
```bash
# 루트에서 .env 파일 만들기
cp ../.env.example ../.env
# .env 열어서 DB_PASSWORD 입력

# DB + Redis 시작
docker-compose up -d

# 실행 확인
docker ps

# 종료
docker-compose down
```

## 접속 정보
- PostgreSQL: localhost:5432 / DB명: drawing_diary
- Redis: localhost:6379
