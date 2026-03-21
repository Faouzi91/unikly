FROM python:3.12-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip for stability
RUN pip install --no-cache-dir --upgrade pip

# Use ARG for HF_TOKEN if provided (can be passed via --build-arg HF_TOKEN=...)
ARG HF_TOKEN
ENV HF_TOKEN=${HF_TOKEN}

# Increase HuggingFace Hub timeouts for slow connections
ENV HF_HUB_DOWNLOAD_TIMEOUT=300
ENV HF_HUB_ETAG_TIMEOUT=60

# Step 1: Install PyTorch CPU alone (the largest part)
RUN pip install --no-cache-dir --default-timeout=1000 --retries 10 \
    torch==2.4.1+cpu --extra-index-url https://download.pytorch.org/whl/cpu

# Step 2: Install the rest of the requirements
COPY requirements.txt .
RUN grep -v "torch" requirements.txt > requirements_no_torch.txt && \
    pip install --no-cache-dir --default-timeout=1000 --retries 10 -r requirements_no_torch.txt

COPY app/ ./app/

# Pre-download the model at build time
# If HF_TOKEN is set, it will be used for faster authenticated download
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"

EXPOSE 8090

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
