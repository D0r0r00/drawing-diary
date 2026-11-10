import json
import statistics
from pathlib import Path

from app.services.relevance_service import evaluate_relevance

# 테스트할 일기
DIARY_TEXT = """
오늘은 날씨가 맑아서 강아지와 함께 공원에 산책을 갔다.
하늘에는 구름이 떠 있었고 나무와 꽃이 예쁘게 피어 있었다.
강아지와 함께 걸으니 즐거운 하루였다.
"""

IMAGE_PATH = Path(__file__).parent / "test.png"

if not IMAGE_PATH.exists():
    raise FileNotFoundError("test.png 파일이 없습니다.")

image_bytes = IMAGE_PATH.read_bytes()

scores = []
results = []

# 동일한 그림일기를 5회 평가
for i in range(5):
    print(f"\n===== {i + 1}회 평가 시작 =====", flush=True)

    result = evaluate_relevance(
        text=DIARY_TEXT,
        image_bytes=image_bytes,
        mime_type="image/png"
    )

    score = result["relevanceScore"]
    scores.append(score)
    results.append(result)

    print(f"{i + 1}회 점수: {score}", flush=True)

# 평균 및 표본 표준편차
mean_score = statistics.mean(scores)
std_score = statistics.stdev(scores)

summary = {
    "evaluationCount": 5,
    "scores": scores,
    "meanScore": round(mean_score, 4),
    "standardDeviation": round(std_score, 4),
    "results": results
}

# JSON 파일로 결과 저장
output_path = Path(__file__).parent / "consistency_result.json"

with open(output_path, "w", encoding="utf-8") as f:
    json.dump(summary, f, ensure_ascii=False, indent=4)

print("\n========== 최종 검증 결과 ==========")
print(f"평가 점수: {scores}")
print(f"평균 점수: {mean_score:.2f}")
print(f"표본 표준편차: {std_score:.4f}")
print(f"결과 저장: {output_path.name}")