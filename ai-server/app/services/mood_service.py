import cv2
import numpy as np
from collections import Counter


def analyze_mood(image_path: str):
    """
    그림의 색채 특성을 분석하여 무드 트래커 데이터를 생성한다.
    """

    # 1. 이미지 불러오기
    image = cv2.imread(image_path)

    if image is None:
        raise ValueError("이미지를 불러올 수 없습니다.")

    # 2. HSV 색 공간으로 변환
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)

    # 3. 흰색 배경 제외
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    drawing_mask = gray < 245

    if not np.any(drawing_mask):
        return {
            "brightness": 0,
            "saturation": 0,
            "achromaticRatio": 0,
            "pleasureIndex": 0,
            "dominantColors": []
        }

    # 4. 그림 영역의 HSV 값 추출
    drawing_hsv = hsv[drawing_mask]

    # 5. 평균 명도 (0~1)
    brightness = np.mean(drawing_hsv[:, 2]) / 255

    # 6. 평균 채도 (0~1)
    saturation = np.mean(drawing_hsv[:, 1]) / 255

    # 7. 무채색 비율
    achromatic_pixels = drawing_hsv[:, 1] < 30
    achromatic_ratio = np.mean(achromatic_pixels)

    # 8. Pleasure 지수 계산
    pleasure_index = (
        0.69 * brightness
        + 0.22 * saturation
    )

    # 9. 주요 색상 추출
    drawing_bgr = image[drawing_mask]

    # 색상을 32단위로 양자화
    quantized = (drawing_bgr // 32) * 32

    # 색상 빈도 계산
    color_counts = Counter(map(tuple, quantized.tolist()))

    dominant_colors = []

    for color, count in color_counts.most_common(5):
        b, g, r = map(int, color)

        dominant_colors.append({
            "hex": f"#{r:02X}{g:02X}{b:02X}",
            "ratio": round(count / len(drawing_bgr), 4)
        })

    return {
        "brightness": round(float(brightness), 4),
        "saturation": round(float(saturation), 4),
        "achromaticRatio": round(float(achromatic_ratio), 4),
        "pleasureIndex": round(float(pleasure_index), 4),
        "dominantColors": dominant_colors
    }