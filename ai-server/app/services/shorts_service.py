import io
import uuid
from pathlib import Path

import cv2
import numpy as np

from PIL import Image, ImageDraw, ImageFont

from app.services.shorts_ai_service import analyze_diary_for_shorts
from app.services.shorts_audio_service import add_audio_to_video


BASE_DIR = Path(__file__).resolve().parents[2]

OUTPUT_DIR = BASE_DIR / "outputs"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)


WIDTH = 540
HEIGHT = 960

FPS = 15
DURATION = 15

FONT_PATH = "C:/Windows/Fonts/malgun.ttf"


def load_font(size=30):

    try:
        return ImageFont.truetype(
            FONT_PATH,
            size
        )

    except Exception:
        return ImageFont.load_default()


def prepare_image(image_bytes):

    try:
        with Image.open(io.BytesIO(image_bytes)) as img:
            img.load()
            return img.convert("RGB")

    except Exception:
        raise ValueError(
            "이미지 파일을 읽을 수 없습니다."
        )


def create_frame(
    image,
    progress,
    scene
):
    """
    Gemini 분석 결과에 따라
    화면 확대 및 이동 효과 적용
    """

    img_width, img_height = image.size

    animation = scene.get(
        "animation",
        "zoom_in"
    )

    focus_x = scene.get("focus_x", 0.5)
    focus_y = scene.get("focus_y", 0.5)

    # 원본 이미지 전체를 세로 영상에 맞추기
    # 원본 비율 유지, 화면은 빈 공간 없이 채움
    base_scale = max(
        WIDTH / img_width,
        HEIGHT / img_height
    )

    # 장면별 확대 비율
    if animation == "zoom_in":

        zoom = 1.0 + progress * 0.25

    elif animation == "zoom_out":

        zoom = 1.25 - progress * 0.25

    else:

        zoom = 1.15

    scale = base_scale * zoom

    new_width = max(
        WIDTH,
        round(img_width * scale)
    )

    new_height = max(
        HEIGHT,
        round(img_height * scale)
    )

    resized = image.resize(
        (new_width, new_height),
        Image.Resampling.LANCZOS
    )

    max_x = new_width - WIDTH
    max_y = new_height - HEIGHT

    # AI가 결정한 중심 위치
    center_x = focus_x * new_width
    center_y = focus_y * new_height

    left = center_x - WIDTH / 2
    top = center_y - HEIGHT / 2

    # 화면 이동 효과
    move_distance = 0.15 * max_x

    if animation == "pan_left":

        left += (
            (0.5 - progress) * 2 * move_distance
        )

    elif animation == "pan_right":

        left += (
            (progress - 0.5) * 2 * move_distance
        )

    left = int(
        max(0, min(left, max_x))
    )

    top = int(
        max(0, min(top, max_y))
    )

    frame = resized.crop((
        left,
        top,
        left + WIDTH,
        top + HEIGHT
    ))

    return frame


def wrap_caption(text, font, max_width):
    """
    자막을 실제 글자 너비 기준으로 줄바꿈
    """

    lines = []
    current = ""

    for char in text:

        candidate = current + char

        if font.getlength(candidate) > max_width and current:
            lines.append(current)
            current = char
        else:
            current = candidate

    if current:
        lines.append(current)

    return lines[:3]


def add_caption(frame, caption):

    font = load_font(30)

    overlay = Image.new(
        "RGBA",
        frame.size,
        (0, 0, 0, 0)
    )

    draw = ImageDraw.Draw(overlay)

    lines = wrap_caption(
        caption,
        font,
        WIDTH - 100
    )

    if not lines:
        return frame

    line_height = 42
    box_height = max(
        100,
        len(lines) * line_height + 40
    )

    box_bottom = HEIGHT - 85
    box_top = box_bottom - box_height

    draw.rounded_rectangle(
        (
            25,
            box_top,
            WIDTH - 25,
            box_bottom
        ),
        radius=20,
        fill=(0, 0, 0, 170)
    )

    y = box_top + 20

    for line in lines:

        text_width = draw.textlength(
            line,
            font=font
        )

        x = (WIDTH - text_width) / 2

        draw.text(
            (x, y),
            line,
            font=font,
            fill="white"
        )

        y += line_height

    result = Image.alpha_composite(
        frame.convert("RGBA"),
        overlay
    )

    return result.convert("RGB")


def generate_shorts(
    image_bytes,
    diary_text
):
    """
    Gemini AI 분석을 활용한
    그림일기 쇼츠 생성
    """

    print("1. Gemini 그림일기 분석 시작")

    # Gemini 호출
    analysis = analyze_diary_for_shorts(
        image_bytes,
        diary_text
    )

    print("2. Gemini 분석 완료")

    import json

    print(
        json.dumps(
            analysis,
            ensure_ascii=False,
            indent=2
        )
    )

    image = prepare_image(image_bytes)

    scenes = analysis["scenes"]

    video_name = (
        f"shorts_{uuid.uuid4().hex}.mp4"
    )

    video_path = OUTPUT_DIR / video_name

    fourcc = cv2.VideoWriter_fourcc(
        *"mp4v"
    )

    video = cv2.VideoWriter(
        str(video_path),
        fourcc,
        FPS,
        (WIDTH, HEIGHT)
    )

    if not video.isOpened():

        raise RuntimeError(
            "MP4 영상 인코더를 열 수 없습니다."
        )

    print("3. 영상 생성 시작")

    try:

        for scene_index, scene in enumerate(scenes):

            duration = scene["duration"]

            frame_count = FPS * duration

            print(
                f"장면 {scene_index + 1} 생성 중:",
                scene.get("focus")
            )

            for frame_index in range(frame_count):

                progress = (
                    frame_index / max(1, frame_count - 1)
                )

                frame = create_frame(
                    image,
                    progress,
                    scene
                )

                caption = scene.get(
                    "caption",
                    ""
                )

                frame = add_caption(
                    frame,
                    caption
                )

                frame_np = np.array(frame)

                frame_bgr = cv2.cvtColor(
                    frame_np,
                    cv2.COLOR_RGB2BGR
                )

                video.write(frame_bgr)

    finally:

        video.release()

    if not video_path.exists() or video_path.stat().st_size == 0:

        raise RuntimeError(
            "영상 파일 생성에 실패했습니다."
        )

    print("4. 쇼츠 영상 생성 완료")

    print("5. 배경음악 및 효과음 생성 시작")

    final_video_path = add_audio_to_video(
        video_path=video_path,
        analysis=analysis
    )

    print("6. 오디오 포함 쇼츠 영상 생성 완료")

    return final_video_path