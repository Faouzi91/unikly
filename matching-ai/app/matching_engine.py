import logging

import numpy as np
from sentence_transformers import SentenceTransformer

from app.config import MAX_MATCHES, MODEL_NAME
from app.models import MatchRequest, MatchResponse, MatchScore

logger = logging.getLogger(__name__)

_model: SentenceTransformer | None = None


def load_model() -> SentenceTransformer:
    global _model
    logger.info("Loading sentence transformer model: %s", MODEL_NAME)
    _model = SentenceTransformer(MODEL_NAME)
    logger.info("Model loaded successfully")
    return _model


def get_model() -> SentenceTransformer:
    if _model is None:
        raise RuntimeError("Model not loaded — call load_model() during startup")
    return _model


def generate_embedding(text: str) -> np.ndarray:
    return get_model().encode(text, convert_to_numpy=True)


def _cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    norm_a = float(np.linalg.norm(a))
    norm_b = float(np.linalg.norm(b))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return float(np.dot(a, b) / (norm_a * norm_b))


def compute_matches(request: MatchRequest) -> MatchResponse:
    job_text = f"{request.job_description} Skills: {', '.join(request.job_skills)}"
    job_embedding = generate_embedding(job_text)

    job_skills_lower = {s.lower() for s in request.job_skills}
    scores: list[MatchScore] = []

    for freelancer in request.freelancers:
        freelancer_text = f"{freelancer.bio} Skills: {', '.join(freelancer.skills)}"
        freelancer_embedding = generate_embedding(freelancer_text)

        raw_score = _cosine_similarity(job_embedding, freelancer_embedding)
        # Cosine similarity for text embeddings is typically [0, 1]; clamp to be safe
        score = max(0.0, min(1.0, raw_score))

        matched_skills = [s for s in freelancer.skills if s.lower() in job_skills_lower]

        scores.append(MatchScore(
            freelancer_id=freelancer.user_id,
            score=round(score, 4),
            matched_skills=matched_skills,
        ))

    scores.sort(key=lambda x: x.score, reverse=True)

    return MatchResponse(job_id=request.job_id, matches=scores[:MAX_MATCHES])
