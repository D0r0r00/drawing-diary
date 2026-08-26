from pydantic import BaseModel, Field


class GuideRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=1,
        description="그림일기 내용"
    )

    style: str = Field(
        ...,
        description="선화 스타일 선택: 1, 2, 3"
    )