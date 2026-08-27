import base64

from app.prompts.guide_prompt import build_guide_prompt
from app.services.gemini_service import client


def generate_guide_image(diary_text: str, style: str) -> bytes:
    prompt = build_guide_prompt(
        diary_text=diary_text,
        style=style
    )

    interaction = client.interactions.create(
        model="gemini-3.1-flash-image",
        input=prompt
    )

    if interaction.output_image is None:
        raise ValueError("선화 이미지를 생성하지 못했습니다.")

    image_bytes = base64.b64decode(
        interaction.output_image.data
    )

    return image_bytes