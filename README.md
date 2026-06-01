# 🎨 Drawing Diary

실시간 협업 캔버스를 활용한 교환 그림 일기 및 소셜 플랫폼

## 팀원 (역할은 임시)
| 역할 | 이름 |
|------|------|
| Frontend | 박연우 |
| Backend | 최민석 |
| AI analyzing | 박지영 |
| DB/INFRA | 이유진 |

## 폴더 구조
```
drawing-diary/
├── frontend/     # React / Next.js (박연우)
├── backend/      # Spring Boot (최민석)
├── ai-server/    # FastAPI + AI API (박지영)
├── infra/        # Docker, DB 설정 (이유진)
└── docs/         # 설계 문서 (ERD, API 명세 등)
```

## 시작하기
1. `.env.example`을 복사해서 `.env` 만들기
2. `cd infra && docker-compose up -d` 로 DB 실행
3. 각 폴더의 README 참고해서 개발 환경 세팅
