import json
import os
import io

from PIL import Image
from dotenv import load_dotenv
from google import genai
from google.genai import types

from app.prompts.shorts_prompt import SHORTS_ANALYSIS_PROMPT


load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

GEMINI_MODEL = os.getenv(
    "GEMINI_SHORTS_MODEL",
    "gemini-3.8-flash"
)


def analyze_diary_for_shorts(image_bytes, diary_text):
    """
    Gemini API를 통해 그림일기 분석 및
    쇼츠 영상 제작 계획 JSON 생성
    """

    if not GEMINI_API_KEY:
        raise ValueError(
            ".env 파일에 GEMINI_API_KEY가 없습니다."
        )

    client = genai.Client(
        api_key=GEMINI_API_KEY
    )

    # 이미지 변환
    with Image.open(io.BytesIO(image_bytes)) as img:
        image = img.convert("RGB")

    # Gemini 프롬프트
    prompt = f"""
{SHORTS_ANALYSIS_PROMPT}

[사용자 일기 내용]

{diary_text}

위 일기와 첨부된 그림을 분석하여
영상 제작 계획을 생성하세요.
"""

    # Gemini API 호출
    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=[
            prompt,
            image
        ],
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            temperature=0.3
        )
    )

    if not response.text:
        raise RuntimeError(
            "Gemini 분석 결과가 비어 있습니다."
        )

    # JSON 변환
    result = json.loads(response.text)

    if not isinstance(result, dict):
        raise ValueError("Gemini 분석 결과 형식이 잘못되었습니다.")

    scenes = result.get("scenes")

    if not isinstance(scenes, list) or len(scenes) != 4:
        raise ValueError("Gemini가 4개 장면을 생성하지 않았습니다.")

    allowed_animations = {
        "zoom_in",
        "zoom_out",
        "pan_left",
        "pan_right"
    }

    durations = [3, 4, 4, 4]

    # Gemini 결과 검증 및 정규화
    for index, scene in enumerate(scenes):

        if not isinstance(scene, dict):
            raise ValueError("장면 데이터 형식이 잘못되었습니다.")

        scene["duration"] = durations[index]

        animation = scene.get("animation", "zoom_in")

        if animation not in allowed_animations:
            animation = "zoom_in"

        scene["animation"] = animation

        # 위치 범위 제한
        for key in ["focus_x", "focus_y"]:

            try:
                value = float(scene.get(key, 0.5))
            except (ValueError, TypeError):
                value = 0.5

            scene[key] = max(0.0, min(1.0, value))

        scene["caption"] = str(
            scene.get("caption", "")
        )[:100]

        scene["focus"] = str(
            scene.get("focus", "그림 전체")
        )

    return result