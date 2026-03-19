# Unikly AI Matching Service

Python FastAPI microservice that ranks freelancers against a job posting using
[sentence-transformers](https://www.sbert.net/) semantic embeddings.

## How it works

1. On startup the `all-MiniLM-L6-v2` model is loaded into memory.
2. `POST /api/ai/match` receives a job description + skills and a list of freelancer
   profiles, computes cosine similarity between their sentence embeddings, and returns
   the top-20 matches sorted by score (0.0–1.0).
3. The Spring Boot Matching Service calls this endpoint and blends the AI score with
   its rule-based score (50 / 50) to produce a hybrid ranking.

## Local development

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8090
```

## Docker

```bash
docker build -t unikly-matching-ai .
docker run -p 8090:8090 unikly-matching-ai
```

## Endpoints

| Method | Path            | Description                   |
|--------|-----------------|-------------------------------|
| POST   | /api/ai/match   | Match freelancers to a job    |
| GET    | /health         | Liveness probe                |
| GET    | /docs           | Interactive Swagger UI        |
