from pydantic import BaseModel


class ScoreResponse(BaseModel):
    relevanceScore: int
    colorScore: int
    feedback: str