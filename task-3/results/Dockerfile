FROM python:3.11-slim-bookworm
WORKDIR /app

# Устанавливаем зависимости глобально (без --user)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Копируем скрипт
COPY export_data.py .

# Создаём пользователя
RUN useradd -m -u 1000 exporter && \
    chown -R exporter:exporter /app && \
    chmod +x /app/export_data.py

USER exporter

ENTRYPOINT ["python", "export_data.py"]