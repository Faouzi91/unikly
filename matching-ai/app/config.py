import os

MODEL_NAME: str = os.getenv("MODEL_NAME", "all-MiniLM-L6-v2")
MAX_MATCHES: int = int(os.getenv("MAX_MATCHES", "20"))
LOG_LEVEL: str = os.getenv("LOG_LEVEL", "info")
