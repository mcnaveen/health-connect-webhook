# Build from repo root (docker-compose sets context: ../..)
FROM python:3.12-slim

WORKDIR /app

COPY server/example/python/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY proto /proto
COPY server/example/python /app

RUN chmod +x /app/generate.sh \
  && ./generate.sh

ENV PORT=50051
EXPOSE 50051

CMD ["python", "main.py"]
