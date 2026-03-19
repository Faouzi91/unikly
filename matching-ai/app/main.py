import logging
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from app.config import MODEL_NAME
from app.matching_engine import compute_matches, load_model
from app.models import MatchRequest, MatchResponse

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):  # noqa: ARG001
    load_model()
    yield


app = FastAPI(
    title="Unikly AI Matching Service",
    version="1.0.0",
    description="Semantic freelancer–job matching using sentence-transformers embeddings",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def request_logging_middleware(request: Request, call_next):
    start = time.perf_counter()
    response = await call_next(request)
    duration_ms = int((time.perf_counter() - start) * 1000)
    logger.info(
        "%s %s status=%d duration=%dms",
        request.method,
        request.url.path,
        response.status_code,
        duration_ms,
    )
    return response


@app.post("/api/ai/match", response_model=MatchResponse)
async def match(request: MatchRequest) -> MatchResponse:
    logger.info(
        "Matching job_id=%s against %d freelancers",
        request.job_id,
        len(request.freelancers),
    )
    return compute_matches(request)


@app.get("/health")
async def health() -> dict:
    return {"status": "ok", "model": MODEL_NAME}
