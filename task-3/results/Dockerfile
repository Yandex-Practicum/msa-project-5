FROM python:3.12-slim

WORKDIR /app

COPY app/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/export_events.py .

# Каталог для выгрузки CSV (сюда монтируется том из пода)
RUN mkdir -p /export

# Работаем не под root
RUN useradd -r -s /bin/false exporter && \
    chown -R exporter:exporter /app /export
USER exporter

ENTRYPOINT ["python", "export_events.py"]
