from app.services.mood_service import analyze_mood
import json

result = analyze_mood("test.png")

print(json.dumps(result, indent=4, ensure_ascii=False))