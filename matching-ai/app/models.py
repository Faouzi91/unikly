from pydantic import BaseModel


class FreelancerProfile(BaseModel):
    user_id: str
    bio: str
    skills: list[str]
    hourly_rate: float
    average_rating: float


class MatchRequest(BaseModel):
    job_id: str
    job_description: str
    job_skills: list[str]
    freelancers: list[FreelancerProfile]


class MatchScore(BaseModel):
    freelancer_id: str
    score: float  # 0.0–1.0
    matched_skills: list[str]


class MatchResponse(BaseModel):
    job_id: str
    matches: list[MatchScore]
