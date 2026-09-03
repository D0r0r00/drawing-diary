from fastapi import APIRouter, File, Form, UploadFile, HTTPException

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
    if not text.strip():
        raise HTTPException(
            status_code=400,
            detail="text는 비어 있을 수 없습니다."
        )


    image_bytes = await image.read()

    if image.content_type not in ["image/jpeg", "image/png", "image/webp"]:
        raise HTTPException(
            status_code=400,
            detail="지원하지 않는 이미지 형식입니다. jpeg, png, webp만 가능합니다."
        )
    
    result = score_diary(
        text=text,
        image_bytes=image_bytes,
        mime_type=image.content_type
    )

    return result