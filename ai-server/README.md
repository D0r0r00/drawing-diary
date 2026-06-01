# AI Server

## 환경 설정
```bash
python -m venv venv
source venv/bin/activate      # Mac/Linux
venv\Scripts\activate         # Windows
pip install -r requirements.txt
```

## 서버 실행
```bash
uvicorn main:app --reload --port 8000
```
→ http://localhost:8000/docs 에서 API 테스트 UI 자동 제공

## API 목록
| Method | URL | 기능 |
|--------|-----|------|
| GET | / | 헬스체크 |
| POST | /extract-keywords | 키워드 추출 |
| POST | /generate-guide | 선화 가이드 생성 |
| POST | /score-diary | AI 점수 산정 |
