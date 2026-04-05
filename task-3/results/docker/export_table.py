"""Экспорт данных из таблицы PostgreSQL в CSV-файл."""

from __future__ import annotations

import csv
import logging
import os
import sys
from datetime import datetime
from typing import Any

import psycopg2

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger: logging.Logger = logging.getLogger(__name__)

# Конфигурация через переменные окружения
DB_HOST: str = os.environ.get("DB_HOST", "localhost")
DB_PORT: str = os.environ.get("DB_PORT", "5432")
DB_NAME: str = os.environ.get("DB_NAME", "logistics")
DB_USER: str = os.environ.get("DB_USER", "logistics")
DB_PASSWORD: str = os.environ.get("DB_PASSWORD", "logistics")
TABLE_NAME: str = os.environ.get("TABLE_NAME", "shipments")
OUTPUT_DIR: str = os.environ.get("OUTPUT_DIR", "/output")

ALLOWED_TABLES: set[str] = {
    "shipments",
    "shipment_events",
    "drivers",
    "vehicles",
    "clients",
}


def export_table(table: str, output_dir: str) -> str:
    """Экспортирует таблицу из PostgreSQL в CSV-файл."""
    if table not in ALLOWED_TABLES:
        raise ValueError(f"Таблица '{table}' не разрешена. Допустимые: {ALLOWED_TABLES}")

    timestamp: str = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename: str = f"{table}_{timestamp}.csv"
    filepath: str = os.path.join(output_dir, filename)

    logger.info("Подключение к БД %s:%s/%s", DB_HOST, DB_PORT, DB_NAME)

    conn: Any = psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
    )

    try:
        cursor: Any = conn.cursor()

        # Получаем названия колонок
        cursor.execute(
            "SELECT column_name FROM information_schema.columns " "WHERE table_name = %s ORDER BY ordinal_position",
            (table,),
        )
        columns: list[str] = [row[0] for row in cursor.fetchall()]

        if not columns:
            raise ValueError(f"Таблица '{table}' не найдена или пуста")

        logger.info("Таблица '%s': колонки %s", table, columns)

        # Экспорт данных
        query: str = f"SELECT * FROM {table}"  # noqa: S608 — table из ALLOWED_TABLES
        cursor.execute(query)

        os.makedirs(output_dir, exist_ok=True)

        rows_count: int = 0
        with open(filepath, "w", newline="", encoding="utf-8") as f:
            writer: csv.writer = csv.writer(f)
            writer.writerow(columns)

            while True:
                rows: list[tuple[Any, ...]] = cursor.fetchmany(5000)
                if not rows:
                    break
                writer.writerows(rows)
                rows_count += len(rows)
                logger.info("Записано %d строк...", rows_count)

        cursor.close()
    finally:
        conn.close()

    logger.info("Экспорт завершен: %s (%d строк)", filepath, rows_count)
    return filepath


def main() -> None:
    """Точка входа."""
    logger.info("Запуск экспорта таблицы '%s'", TABLE_NAME)

    try:
        result: str = export_table(TABLE_NAME, OUTPUT_DIR)
        logger.info("Файл создан: %s", result)
    except Exception:
        logger.exception("Ошибка экспорта")
        sys.exit(1)

    logger.info("Готово")


if __name__ == "__main__":
    main()
