FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY trigger_etl.py .

ENV LOG_FILE=/var/log/batch/client.log

ENTRYPOINT ["python", "/app/trigger_etl.py"]

