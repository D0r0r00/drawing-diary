from fastapi import APIRouter, File, Form, UploadFile

from app.schemas.score import ScoreResponse
from app.services.score_service import score_diary


router = APIRouter(
    prefix="/api/ai",
    tags=["AI Score"]
)


@router.post("/score", response_model=ScoreResponse)
async def evaluate_diary(
    text: str = Form(...),
    image: UploadFile = File(...)
):
    image_bytes = await image.read()

    result = score_diary(
        text=text,
        image_bytes=image_bytes,
        mime_type=image.content_type
    )

    return result