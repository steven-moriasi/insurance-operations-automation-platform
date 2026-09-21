FROM python:3.12.11-slim

RUN useradd --system --uid 10001 --create-home insurance
WORKDIR /app
COPY workers/automation-worker ./
RUN pip install --no-cache-dir .
USER 10001
ENTRYPOINT ["insurance-automation-worker"]
