from pydantic import BaseModel
from google.genai import types

from app.services.gemini_service import client


class GeminiScoreResult(BaseModel):
    relationScore: int
    colorScore: int
    feedback: str


def score_diary(text: str, image_bytes: bytes, mime_type: str):

    prompt = f"""
다음은 사용자가 작성한 그림일기입니다.

[일기 내용]
{text}

첨부된 그림과 일기 내용을 함께 분석하세요.

평가 기준:

1. relationScore
- 일기 텍스트와 그림 내용의 연관성
- 0점부터 100점까지 평가

2. colorScore
- 그림에 사용된 색채의 다양성
- 0점부터 100점까지 평가

3. feedback
- 평가 결과를 한국어로 한두 문장 작성
"""

    image_part = types.Part.from_bytes(
        data=image_bytes,
        mime_type=mime_type
    )

    response = client.models.generate_content(
        model="gemini-3.6-flash",
        contents=[
            prompt,
            image_part
        ],
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=GeminiScoreResult
        )
    )

    result = response.parsed

    if result is None:
        raise ValueError("Gemini 평가 결과를 파싱하지 못했습니다.")

    relation_score = result.relationScore
    color_score = result.colorScore

    total_score = round(
        relation_score * 0.6 +
        color_score * 0.4
    )

    return {
        "totalScore": total_score,
        "relationScore": relation_score,
        "colorScore": color_score,
        "feedback": result.feedback
    }