from pydantic import BaseModel
from google.genai import types

from app.services.gemini_service import client


class GeminiScoreResult(BaseModel):
    relationScore: int
    colorScore: int
    feedback: str


def score_diary(text: str, image_bytes: bytes, mime_type: str, like_count: int):

    prompt = f"""
다음은 사용자가 작성한 그림일기입니다.

[일기 내용]
{text}

첨부된 그림과 일기 내용을 함께 분석하세요.

평가는 반드시 아래 두 항목만 수행하세요.

1. relationScore - 텍스트-이미지 연관성

사용자가 작성한 일기 내용과 그림 이미지가 얼마나 잘 연결되어 있는지 평가합니다.

다음 내용을 중심으로 판단하세요.
- 일기에 등장하는 주요 인물, 장소, 사물 등이 그림에 표현되어 있는가
- 일기의 핵심 사건이나 상황이 그림에서 확인되는가
- 일기의 전체적인 내용과 그림의 장면이 자연스럽게 연결되는가
- 일기와 관련 없는 요소가 그림의 중심을 차지하고 있지는 않은가

점수 기준:
- 90~100점: 일기의 핵심 내용과 주요 요소가 그림에 매우 잘 표현되어 있음
- 70~89점: 주요 내용 대부분이 그림과 관련되어 있으나 일부 요소가 빠져 있음
- 50~69점: 일기와 그림의 관련성은 있으나 표현된 내용이 제한적임
- 30~49점: 일부 요소만 관련되어 있고 전체적인 연결성이 낮음
- 0~29점: 일기 내용과 그림이 거의 관련이 없음

중요:
그림의 화풍, 그림 실력, 완성도 자체는 평가하지 말고
일기 내용과 그림의 의미적 연관성만 평가하세요.


2. colorScore - 색채 다양성

그림에서 사용된 색상의 다양성을 평가합니다.

다음 내용을 중심으로 판단하세요.
- 서로 구분되는 여러 색상이 사용되었는가
- 특정 한두 가지 색상에 지나치게 편중되어 있지는 않은가
- 인물, 배경, 사물 등에 다양한 색상이 활용되어 있는가
- 그림 전체에서 색상 변화가 충분히 나타나는가

점수 기준:
- 90~100점: 매우 다양한 색상이 그림 전반에 폭넓게 사용됨
- 70~89점: 여러 색상이 사용되며 색채 구성이 비교적 다양함
- 50~69점: 일정 수준의 색상 다양성은 있으나 일부 색상에 편중됨
- 30~49점: 사용된 색상이 적고 색채 변화가 제한적임
- 0~29점: 단색 또는 거의 한두 가지 색상만 사용됨

중요:
색이 아름다운지, 조화로운지, 그림이 잘 그려졌는지는 평가하지 말고
실제로 얼마나 다양한 색상이 사용되었는지를 중심으로 평가하세요.


3. feedback

relationScore와 colorScore의 평가 결과를 바탕으로
사용자가 이해하기 쉬운 한국어 피드백을 한두 문장으로 작성하세요.

피드백에서는 그림 실력 자체를 평가하지 말고,
일기와 그림의 연관성과 색상 다양성에 대해서만 설명하세요.
점수를 관대하게 부여하지 말고, 위 점수 구간의 기준을 엄격하게 적용하세요.
명확한 근거가 없는 경우 높은 점수를 부여하지 마세요.
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

    like_score = min(like_count * 5, 100)

    total_score = round(
        relation_score * 0.5 +
        color_score * 0.3 +
        like_score * 0.2
    )

    return {
        "totalScore": total_score,
        "relationScore": relation_score,
        "colorScore": color_score,
        "likeScore": like_score,
        "feedback": result.feedback
    }