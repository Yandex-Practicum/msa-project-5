FROM python:3.12-slim

WORKDIR /app

COPY app/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/export_shipments.py .

RUN mkdir /data

CMD ["python", "export_shipments.py"]
