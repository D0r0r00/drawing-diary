from fastapi import APIRouter, UploadFile, File, HTTPException

from app.services.visual_service import analyze_image

import os
import tempfile


router = APIRouter(
    prefix="/api/ai",
    tags=["AI Visual"]
)


@router.post("/visual")
async def analyze_diary_visual(
    file: UploadFile = File(...)
):
    # 1. 이미지 형식 확인
    allowed_types = [
        "image/png",
        "image/jpeg"
    ]

    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail="PNG 또는 JPEG 이미지만 업로드할 수 있습니다."
        )

    # 2. 이미지 읽기
    image_data = await file.read()

    if not image_data:
        raise HTTPException(
            status_code=400,
            detail="이미지 파일이 비어 있습니다."
        )

    # 3. 이미지 크기 확인
    if len(image_data) > 10 * 1024 * 1024:
        raise HTTPException(
            status_code=413,
            detail="이미지는 10MB 이하여야 합니다."
        )

    temp_path = None

    try:
        # 4. 임시 이미지 파일 생성
        with tempfile.NamedTemporaryFile(
            delete=False,
            suffix=".png"
        ) as temp_file:
            temp_file.write(image_data)
            temp_path = temp_file.name

        # 5. OpenCV 이미지 분석
        result = analyze_image(temp_path)

        return result

    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    finally:
        # 6. 임시 파일 삭제
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)