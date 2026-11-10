import cv2
import numpy as np


def analyze_image(image_path: str):

    # ==========================================
    # 1. 이미지 불러오기
    # ==========================================

    image = cv2.imread(
        image_path,
        cv2.IMREAD_UNCHANGED
    )

    if image is None:
        raise ValueError("이미지를 불러올 수 없습니다.")

    # 투명 배경 처리
    if len(image.shape) == 2:
        image = cv2.cvtColor(
            image,
            cv2.COLOR_GRAY2BGR
        )

    elif image.shape[2] == 4:
        alpha = image[:, :, 3:4] / 255.0

        image = (
            image[:, :, :3] * alpha
            + 255 * (1 - alpha)
        ).astype(np.uint8)

    height, width = image.shape[:2]

    # ==========================================
    # 2. HSV 변환 및 그림 영역 추출
    # ==========================================

    hsv = cv2.cvtColor(
        image,
        cv2.COLOR_BGR2HSV
    )

    gray = cv2.cvtColor(
        image,
        cv2.COLOR_BGR2GRAY
    )

    # 흰색 배경 제외
    drawing_mask = gray < 245

    drawing_pixels = np.count_nonzero(
        drawing_mask
    )

    if drawing_pixels == 0:
        return {
            "colorRatio": 0,
            "colorEntropy": 0,
            "colorHarmony": None,
            "canvasUsage": 0,
            "pixelCoverage": 0,
            "centerX": 0,
            "centerY": 0,
            "spatialDistribution": [
                [0] * 3 for _ in range(3)
            ]
        }

    # ==========================================
    # 3. 채색 비율
    # ==========================================

    saturation = hsv[:, :, 1]

    colored_mask = (
        (saturation > 30)
        & drawing_mask
    )

    ys, xs = np.where(drawing_mask)

    x_min = xs.min()
    x_max = xs.max()
    y_min = ys.min()
    y_max = ys.max()

    bounding_width = x_max - x_min + 1
    bounding_height = y_max - y_min + 1

    bounding_area = (
        bounding_width * bounding_height
    )

    colored_pixels = np.count_nonzero(
        colored_mask
    )

    # 바운딩 박스 내 채색 비율
    color_ratio = (
        colored_pixels / bounding_area
    )

    # ==========================================
    # 4. 색채 엔트로피
    # ==========================================

    hue_values = hsv[:, :, 0][colored_mask]

    if hue_values.size > 0:

        hist, _ = np.histogram(
            hue_values,
            bins=18,
            range=(0, 180)
        )

        probabilities = (
            hist[hist > 0] / hist.sum()
        )

        entropy = -np.sum(
            probabilities
            * np.log(probabilities)
        )

        entropy /= np.log(18)

    else:
        entropy = 0

    # ==========================================
    # 5. 색채 조화도 (보조 지표)
    # ==========================================

    # 주요 색상의 Hue 분포를 이용한
    # 간단한 색상 집중도 지표

    if hue_values.size > 0:

        hist, _ = np.histogram(
            hue_values,
            bins=18,
            range=(0, 180)
        )

        # 가장 많이 사용된 색상 구간 비율
        dominant_ratio = (
            hist.max() / hist.sum()
        )

        color_harmony = float(
            dominant_ratio
        )

    else:
        color_harmony = None

    # ==========================================
    # 6. 화면 사용률
    # ==========================================

    canvas_area = width * height

    # 그림 바운딩 박스 면적 비율
    canvas_usage = (
        bounding_area / canvas_area
    )

    # 실제 그림 픽셀 점유율
    pixel_coverage = (
        drawing_pixels / canvas_area
    )

    # ==========================================
    # 7. 그림 중심 위치
    # ==========================================

    center_x = (
        xs.mean() / width
    )

    center_y = (
        ys.mean() / height
    )

    # ==========================================
    # 8. 3×3 공간 분포
    # ==========================================

    spatial_distribution = []

    for row in range(3):

        row_values = []

        for col in range(3):

            y1 = row * height // 3
            y2 = (row + 1) * height // 3

            x1 = col * width // 3
            x2 = (col + 1) * width // 3

            region = drawing_mask[
                y1:y2,
                x1:x2
            ]

            ratio = (
                np.count_nonzero(region)
                / region.size
            )

            row_values.append(
                round(float(ratio), 4)
            )

        spatial_distribution.append(
            row_values
        )

    # ==========================================
    # 9. 분석 결과 반환
    # ==========================================

    return {
        "colorRatio": round(
            float(color_ratio), 4
        ),
        "colorEntropy": round(
            float(entropy), 4
        ),
        "colorHarmony": (
            round(color_harmony, 4)
            if color_harmony is not None
            else None
        ),
        "canvasUsage": round(
            float(canvas_usage), 4
        ),
        "pixelCoverage": round(
            float(pixel_coverage), 4
        ),
        "centerX": round(
            float(center_x), 4
        ),
        "centerY": round(
            float(center_y), 4
        ),
        "spatialDistribution": (
            spatial_distribution
        )
    }