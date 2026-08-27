from fastapi import APIRouter, HTTPException
from fastapi.responses import Response

from app.schemas.guide import GuideRequest
from app.services.guide_service import generate_guide_image


router = APIRouter(
    prefix="/api/ai",
    tags=["AI Guide"]
)


@router.post("/guide")
def generate_guide(request: GuideRequest):
    try:
        image_bytes = generate_guide_image(
            diary_text=request.text,
            style=request.style
        )

        return Response(
            content=image_bytes,
            media_type="image/png"
        )

    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"선화 생성 중 오류가 발생했습니다: {str(e)}"
        )