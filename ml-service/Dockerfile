FROM python:3.11-slim AS base

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 build-essential curl && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Generate gRPC stubs from proto
COPY proto/ proto/
RUN python -m grpc_tools.protoc \
    -I./proto \
    --python_out=./scoring \
    --grpc_python_out=./scoring \
    proto/scoring.proto

COPY scoring/ scoring/
COPY fraud/    fraud/
COPY fairness/ fairness/
COPY training/ training/

ENV PYTHONPATH=/app
EXPOSE 50051 9090

CMD ["python", "scoring/server.py"]
