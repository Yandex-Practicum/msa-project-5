# Базовый образ
FROM python:3.10-slim

RUN pip install psycopg2-binary

COPY export.py /app/export.py

WORKDIR /app

CMD ["python", "export.py"]