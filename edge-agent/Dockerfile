# ---------------------------------------------------------------------------
# Dockerfile — Multi-stage build for the Edge Collection Agent
# ---------------------------------------------------------------------------
# Stage 1: Build the C extension
# Stage 2: Slim runtime image
# ---------------------------------------------------------------------------

# ---- Stage 1: Builder ----
FROM python:3.12-slim AS builder

RUN apt-get update && \
    apt-get install -y --no-install-recommends gcc libc6-dev && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

COPY setup.py pyproject.toml ./
COPY src/ src/

# Build the C extension in-place so it lives next to the Python sources
RUN pip install --no-cache-dir --prefix=/install . && \
    python setup.py build_ext --inplace

# ---- Stage 2: Runtime ----
FROM python:3.12-slim AS runtime

LABEL maintainer="log-aggregator-team"
LABEL description="Edge Collection Agent for Distributed Log Aggregator"

# Create a non-root user
RUN groupadd -r agent && useradd -r -g agent -d /app -s /sbin/nologin agent

# Copy installed packages from builder
COPY --from=builder /install /usr/local

WORKDIR /app

# Copy application code (including compiled .so)
COPY --from=builder /build/src/ src/
COPY config/ config/

# Create directories for logs and recovery cache
RUN mkdir -p /var/log/log-aggregator /tmp/log-aggregator-recovery && \
    chown -R agent:agent /app /var/log/log-aggregator /tmp/log-aggregator-recovery

USER agent

# Health-check endpoint (default port 8090)
EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8090/health')" || exit 1

ENTRYPOINT ["python", "-m", "src.main"]
CMD ["--config", "config/agent_config.yaml"]
