from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from fastapi.responses import FileResponse

from app.services.shorts_service import generate_shorts

router = APIRouter(prefix="/ai", tags=["AI Shorts"])


@router.post("/shorts")
async def create_shorts(
    image: UploadFile = File(...),
    diary_text: str = Form(...)
):
    """
    그림일기 이미지와 텍스트를 입력받아
    15초 분량의 MP4 쇼츠 영상 생성
    """

    if not image.content_type or image.content_type not in [
        "image/png",
        "image/jpeg"
    ]:
        raise HTTPException(
            status_code=400,
            detail="PNG 또는 JPG 이미지만 업로드할 수 있습니다."
        )

    if not diary_text.strip():
        raise HTTPException(
            status_code=400,
            detail="일기 내용을 입력해주세요."
        )

    image_bytes = await image.read()

    if len(image_bytes) > 10 * 1024 * 1024:
        raise HTTPException(
            status_code=413,
            detail="이미지는 10MB 이하만 업로드할 수 있습니다."
        )

    try:
        video_path = generate_shorts(
            image_bytes=image_bytes,
            diary_text=diary_text
        )

        return FileResponse(
            path=str(video_path),
            media_type="video/mp4",
            filename="drawing_diary_shorts.mp4"
        )

    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:
        print(f"쇼츠 생성 오류: {e}")

        raise HTTPException(
            status_code=500,
            detail="쇼츠 영상 생성 중 오류가 발생했습니다."
        )
