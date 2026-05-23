# Dockerfile
FROM python:3.11-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libpq-dev \
        gcc \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY export.py .

RUN useradd --create-home --shell /bin/bash exporter && \
    chown -R exporter:exporter /app
USER exporter

# Точка входа
ENTRYPOINT ["python", "export.py"]