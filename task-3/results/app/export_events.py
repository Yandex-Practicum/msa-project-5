#!/usr/bin/env python3
"""Ежедневный экспорт событий перевозок (shipment_events) в CSV-файл.

Скрипт запускается k8s CronJob каждый день в 20:00 и выгружает события
за текущую дату в примонтированный каталог для последующей загрузки
в аналитическое хранилище.
"""

import logging
import os
import sys
import time
from datetime import date

import psycopg2

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    stream=sys.stdout,
)
log = logging.getLogger("events-exporter")

DB_DSN = {
    "host": os.environ.get("PGHOST", "localhost"),
    "port": int(os.environ.get("PGPORT", "5432")),
    "dbname": os.environ.get("PGDATABASE", "cargo_ops"),
    "user": os.environ.get("PGUSER", "exporter"),
    "password": os.environ.get("PGPASSWORD", "exporter_secret"),
}
EXPORT_DIR = os.environ.get("EXPORT_DIR", "/export")
CONNECT_RETRIES = 10
CONNECT_DELAY_SEC = 3

# Серверный COPY выгружает дневной срез событий напрямую в поток,
# без загрузки всего объёма (0.5-1 млн строк) в память процесса.
COPY_QUERY = """
COPY (
    SELECT
        event_id,
        shipment_id,
        event_type,
        city,
        details,
        happened_at
    FROM shipment_events
    WHERE happened_at::date = CURRENT_DATE
    ORDER BY happened_at
) TO STDOUT WITH (FORMAT csv, HEADER true, DELIMITER ';')
"""

COUNT_QUERY = """
SELECT COUNT(*) FROM shipment_events WHERE happened_at::date = CURRENT_DATE
"""


def wait_for_database():
    """Ждёт готовности PostgreSQL (CronJob может стартовать раньше БД)."""
    for attempt in range(1, CONNECT_RETRIES + 1):
        try:
            conn = psycopg2.connect(**DB_DSN)
            log.info("Подключение к PostgreSQL установлено (попытка %d)", attempt)
            return conn
        except psycopg2.OperationalError as exc:
            log.warning(
                "БД недоступна, попытка %d/%d: %s", attempt, CONNECT_RETRIES, exc
            )
            time.sleep(CONNECT_DELAY_SEC)
    raise RuntimeError("Не удалось подключиться к PostgreSQL")


def export_today_events(conn) -> str:
    os.makedirs(EXPORT_DIR, exist_ok=True)
    filename = f"shipment_events_{date.today().isoformat()}.csv"
    filepath = os.path.join(EXPORT_DIR, filename)

    with conn.cursor() as cur:
        cur.execute(COUNT_QUERY)
        expected = cur.fetchone()[0]

        log.info("Выгружаю %d событий в %s", expected, filepath)
        with open(filepath, "w", encoding="utf-8") as out:
            cur.copy_expert(COPY_QUERY, out)

    size_kb = os.path.getsize(filepath) / 1024
    log.info("Экспорт завершён: %d строк, файл %.1f КБ", expected, size_kb)
    return filepath


def main():
    log.info("Старт экспорта shipment_events за %s", date.today().isoformat())
    try:
        conn = wait_for_database()
        try:
            export_today_events(conn)
        finally:
            conn.close()
    except Exception as exc:
        log.error("Экспорт завершился ошибкой: %s", exc)
        sys.exit(1)
    log.info("Задача выполнена успешно")


if __name__ == "__main__":
    main()
