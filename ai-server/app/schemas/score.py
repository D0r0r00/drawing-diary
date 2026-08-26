from pydantic import BaseModel


class ScoreResponse(BaseModel):
    totalScore: int
    relationScore: int
    colorScore: int
    feedback: str