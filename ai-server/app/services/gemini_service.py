import os

from dotenv import load_dotenv
from google import genai


load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY가 설정되어 있지 않습니다.")

client = genai.Client(api_key=api_key)


def test_gemini():
    response = client.models.generate_content(
        model="gemini-3.6-flash",
        contents="안녕. Drawing Diary AI 서버 연결 테스트 중이야. 짧게 대답해줘."
    )

    return response.text