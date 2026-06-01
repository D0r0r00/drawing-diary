from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import google.generativeai as genai
import requests
import os
import json
import re
import base64
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Drawing Diary AI Server")

genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
HF_TOKEN = os.getenv("HUGGINGFACE_API_TOKEN")


class DiaryTextRequest(BaseModel):
    text: str

class KeywordResponse(BaseModel):
    keywords: list[str]

class GuideResponse(BaseModel):
    image_base64: str
    prompt_used: str

class ScoreRequest(BaseModel):
    text: str
    color_count: int = 0

class ScoreResponse(BaseModel):
    total_score: int
    emotion_score: int
    completion_score: int
    color_score: int


@app.get("/")
def health_check():
    return {"status": "AI 서버 정상 작동 중"}


@app.post("/extract-keywords", response_model=KeywordResponse)
async def extract_keywords(req: DiaryTextRequest):
    model = genai.GenerativeModel("gemini-2.0-flash")
    prompt = f"""다음 일기에서 그림으로 표현할 핵심 명사와 동사만 JSON 배열로 추출하세요.
예시: ["한강", "자전거", "친구", "타다"]
일기: {req.text}"""
    try:
        response = model.generate_content(prompt)
        match = re.search(r'\[.*?\]', response.text, re.DOTALL)
        keywords = json.loads(match.group()) if match else []
        return KeywordResponse(keywords=keywords)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/generate-guide", response_model=GuideResponse)
async def generate_guide(req: DiaryTextRequest):
    keyword_res = await extract_keywords(req)
    model = genai.GenerativeModel("gemini-2.0-flash")
    prompt = f"Translate to English image prompt (max 10 words): {keyword_res.keywords}"
    translated = model.generate_content(prompt).text.strip()
    final_prompt = f"line art sketch, {translated}, black outline only, minimalist, white background"
    api_url = "https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-schnell"
    headers = {"Authorization": f"Bearer {HF_TOKEN}"}
    try:
        response = requests.post(api_url, headers=headers, json={"inputs": final_prompt}, timeout=30)
        if response.status_code == 200:
            img_b64 = base64.b64encode(response.content).decode()
            return GuideResponse(image_base64=img_b64, prompt_used=final_prompt)
        raise HTTPException(status_code=502, detail="이미지 생성 API 오류")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/score-diary", response_model=ScoreResponse)
async def score_diary(req: ScoreRequest):
    model = genai.GenerativeModel("gemini-2.0-flash")
    prompt = f"""일기를 0~100점으로 평가하세요. JSON만 반환하세요.
일기: {req.text}
색상 수: {req.color_count}
형식: {{"emotion_score": 80, "completion_score": 70, "color_score": 60}}"""
    try:
        response = model.generate_content(prompt)
        match = re.search(r'\{.*?\}', response.text, re.DOTALL)
        scores = json.loads(match.group()) if match else {}
        e = scores.get("emotion_score", 0)
        c = scores.get("completion_score", 0)
        col = scores.get("color_score", 0)
        return ScoreResponse(total_score=int((e+c+col)/3), emotion_score=e, completion_score=c, color_score=col)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
