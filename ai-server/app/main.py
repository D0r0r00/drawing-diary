from fastapi import FastAPI

from app.routers.score import router as score_router
from app.routers.guide import router as guide_router
from app.routers.shorts import router as shorts_router
from app.routers.mood import router as mood_router
from app.routers.relevance import router as relevance_router
from app.routers.visual import router as visual_router

app = FastAPI(
    title="Drawing Diary AI Server",
    version="1.0.0"
)


@app.get("/")
def root():
    return {"status": "AI 서버 정상 작동 중"}


app.include_router(score_router)
app.include_router(guide_router)
app.include_router(shorts_router)
app.include_router(mood_router)
app.include_router(relevance_router)
app.include_router(visual_router)