from fastapi import (
    APIRouter,
    UploadFile,
    File,
    Form,
    HTTPException
)

from app.services.relevance_service import evaluate_relevance


router = APIRouter(
    prefix="/api/ai",
    tags=["AI Relevance"]
)


@router.post("/relevance")
async def evaluate_diary_relevance(
    text: str = Form(...),
    image: UploadFile = File(...)
):

    # 일기 내용 확인
    if not text.strip():
        raise HTTPException(
            status_code=400,
            detail="일기 내용을 입력하세요."
        )

    # 이미지 형식 확인
    allowed_types = [
        "image/png",
        "image/jpeg",
        "image/webp"
    ]

    if image.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail="지원하지 않는 이미지 형식입니다."
        )

    # 이미지 읽기
    image_bytes = await image.read()

    if not image_bytes:
        raise HTTPException(
            status_code=400,
            detail="이미지 파일이 비어 있습니다."
        )

    if len(image_bytes) > 10 * 1024 * 1024:
        raise HTTPException(
            status_code=413,
            detail="이미지는 10MB 이하여야 합니다."
        )

    # AI 그림 전달력 평가
    result = evaluate_relevance(
        text=text,
        image_bytes=image_bytes,
        mime_type=image.content_type
    )

    return result