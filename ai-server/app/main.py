from fastapi import FastAPI

from app.routers.score import router as score_router
from app.routers.guide import router as guide_router


app = FastAPI(
    title="Drawing Diary AI Server",
    version="1.0.0"
)


@app.get("/")
def root():
    return {"status": "AI 서버 정상 작동 중"}


app.include_router(score_router)
app.include_router(guide_router)