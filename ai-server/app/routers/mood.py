from fastapi import APIRouter, UploadFile, File, HTTPException
from app.services.mood_service import analyze_mood

import os
import tempfile

router = APIRouter(
    prefix="/api/ai",
    tags=["AI Mood"]
)


@router.post("/mood")
async def analyze_diary_mood(file: UploadFile = File(...)):

    if file.content_type not in ["image/png", "image/jpeg"]:
        raise HTTPException(
            status_code=400,
            detail="PNG 또는 JPEG 이미지만 업로드할 수 있습니다."
        )

    image_data = await file.read()

    if not image_data:
        raise HTTPException(
            status_code=400,
            detail="빈 이미지 파일입니다."
        )

    if len(image_data) > 10 * 1024 * 1024:
        raise HTTPException(
            status_code=413,
            detail="이미지 크기는 10MB 이하여야 합니다."
        )

    temp_path = None

    try:
        with tempfile.NamedTemporaryFile(
            delete=False,
            suffix=".png"
        ) as temp_file:
            temp_file.write(image_data)
            temp_path = temp_file.name

        result = analyze_mood(temp_path)
        return result

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    finally:
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)