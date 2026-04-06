FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt /app/requirements.txt

RUN pip install --no-cache-dir -r /app/requirements.txt

COPY export.py /app/export.py

ENTRYPOINT ["python", "/app/export.py"]
