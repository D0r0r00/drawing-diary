import math

from pydantic import BaseModel, Field
from google.genai import types

from app.services.gemini_service import client


# 기존 프로젝트에서 사용하는 Gemini 모델
GEMINI_MODEL = "gemini-3.6-flash"

# 의미 유사도 계산용 임베딩 모델
EMBEDDING_MODEL = "gemini-embedding-001"


# ==========================================
# 1. 응답 데이터 구조
# ==========================================

class DiaryElements(BaseModel):
    elements: list[str]


class ImageDescription(BaseModel):
    description: str


class MatchedElements(BaseModel):
    matchedElements: list[str] = Field(
        description="그림에서 확인되는 원본 일기의 핵심 요소"
    )


# ==========================================
# 2. 원본 일기 핵심 요소 추출
# ==========================================

def extract_diary_elements(text: str):

    prompt = f"""
다음 그림일기에서 그림으로 표현할 수 있는
핵심 요소를 추출하세요.

[원본 일기]
{text}

추출 대상:
- 인물
- 장소
- 행동
- 사물
- 동물

규칙:
1. 그림에서 시각적으로 확인할 수 있는 요소만 추출합니다.
2. 감정이나 추상적인 표현은 제외합니다.
3. 중복된 요소는 제거합니다.
4. 핵심 요소는 최대 10개까지 추출합니다.
5. 각 요소는 간결한 한국어로 작성합니다.

예시:
입력: 오늘 친구와 공원에서 강아지와 산책했다.

출력:
친구
공원
강아지
산책
"""

    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=DiaryElements
        )
    )

    if response.parsed is None:
        raise ValueError("일기 핵심 요소 추출 실패")

    return response.parsed.elements


# ==========================================
# 3. 그림만 보고 장면 설명 생성
# ==========================================

def describe_image(
    image_bytes: bytes,
    mime_type: str
):

    prompt = """
첨부된 그림을 보고 장면을 설명하세요.

중요:
원본 일기의 내용은 제공되지 않습니다.

그림에 실제로 보이는 내용만 분석하세요.

분석 대상:
- 등장인물
- 장소
- 행동
- 사물
- 동물

규칙:
1. 그림에 없는 내용을 추측하지 마세요.
2. 인물의 감정이나 관계를 임의로 판단하지 마세요.
3. 확인 가능한 장면을 한국어로 설명하세요.
4. 그림 실력이나 완성도는 평가하지 마세요.
5. 한두 문장으로 작성하세요.
"""

    image_part = types.Part.from_bytes(
        data=image_bytes,
        mime_type=mime_type
    )

    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=[
            prompt,
            image_part
        ],
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=ImageDescription
        )
    )

    if response.parsed is None:
        raise ValueError("그림 장면 설명 생성 실패")

    return response.parsed.description


# ==========================================
# 4. 핵심 요소 일치율 계산
# ==========================================

def calculate_element_match(
    diary_elements: list[str],
    image_description: str
):

    if not diary_elements:
        return 0.0, []

    prompt = f"""
다음은 원본 일기에서 추출한 핵심 요소와
그림을 보고 생성한 장면 설명입니다.

[원본 일기 핵심 요소]
{diary_elements}

[그림 장면 설명]
{image_description}

그림 장면 설명에서 확인할 수 있는
원본 일기의 핵심 요소를 선택하세요.

규칙:
1. 원본 핵심 요소 목록에 있는 것만 반환합니다.
2. 표현이 달라도 의미가 같다면 일치로 판단합니다.
3. 장면 설명만으로 확인할 수 없는 요소는 제외합니다.
4. 새로운 요소를 만들어 내지 마세요.
5. 근거가 불확실하다면 일치시키지 마세요.
"""

    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=MatchedElements
        )
    )

    if response.parsed is None:
        raise ValueError("핵심 요소 비교 실패")

    # 모델이 반환한 요소 중 원본 목록에 있는 것만 인정
    selected = set(response.parsed.matchedElements)

    matched = [
        element
        for element in diary_elements
        if element in selected
    ]

    # 중복 제거
    matched = list(dict.fromkeys(matched))
    total = len(set(diary_elements))

    match_ratio = len(matched) / total

    return round(match_ratio, 4), matched


# ==========================================
# 5. 임베딩 의미 유사도 계산
# ==========================================

def calculate_semantic_similarity(
    diary_text: str,
    image_description: str
):

    response = client.models.embed_content(
        model=EMBEDDING_MODEL,
        contents=[
            diary_text,
            image_description
        ],
        config=types.EmbedContentConfig(
            task_type="SEMANTIC_SIMILARITY"
        )
    )

    if not response.embeddings or len(response.embeddings) != 2:
        raise ValueError("임베딩 생성 실패")

    vector1 = response.embeddings[0].values
    vector2 = response.embeddings[1].values

    if not vector1 or not vector2:
        raise ValueError("임베딩 벡터가 비어 있습니다.")

    dot_product = sum(
        a * b
        for a, b in zip(vector1, vector2)
    )

    norm1 = math.sqrt(
        sum(a * a for a in vector1)
    )

    norm2 = math.sqrt(
        sum(b * b for b in vector2)
    )

    if norm1 == 0 or norm2 == 0:
        return 0.0

    similarity = dot_product / (norm1 * norm2)

    # 점수 산출을 위해 음수 유사도는 0으로 처리
    similarity = max(0.0, min(1.0, similarity))

    return round(similarity, 4)


# ==========================================
# 6. 최종 그림 전달력 평가
# ==========================================

def evaluate_relevance(
    text: str,
    image_bytes: bytes,
    mime_type: str
):

    # 1. 일기 핵심 요소 추출
    diary_elements = extract_diary_elements(text)

    # 2. 그림만 보고 설명 생성
    image_description = describe_image(
        image_bytes,
        mime_type
    )

    # 3. 핵심 요소 일치율
    match_ratio, matched_elements = (
        calculate_element_match(
            diary_elements,
            image_description
        )
    )

    # 4. 의미 유사도
    semantic_similarity = (
        calculate_semantic_similarity(
            text,
            image_description
        )
    )

    # 5. 최종 전달력 점수
    final_score = (
        0.7 * match_ratio
        + 0.3 * semantic_similarity
    ) * 100

    return {
        "diaryElements": diary_elements,
        "imageDescription": image_description,
        "matchedElements": matched_elements,
        "elementMatchRatio": match_ratio,
        "semanticSimilarity": semantic_similarity,
        "relevanceScore": round(final_score, 2)
    }