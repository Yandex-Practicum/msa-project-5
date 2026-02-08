FROM python:3.10-slim

WORKDIR /app

COPY . .

RUN pip install psycopg2-binary

CMD ["python", "export.py"]
