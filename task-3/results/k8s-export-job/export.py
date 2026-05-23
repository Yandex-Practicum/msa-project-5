#!/usr/bin/env python3
"""
Ежедневный экспорт данных грузоперевозок в CSV.
Поддерживает экспорт отдельных таблиц или джойнов.

Переменные окружения:
    DB_HOST          - Хост PostgreSQL
    DB_PORT          - Порт PostgreSQL (по умолчанию 5432)
    DB_NAME          - Имя базы данных
    DB_USER          - Пользователь
    DB_PASSWORD      - Пароль
    EXPORT_TABLE     - Имя таблицы для экспорта (по умолчанию shipments)
    EXPORT_DATE      - Дата в формате YYYY-MM-DD (по умолчанию сегодня)
    OUTPUT_DIR       - Директория для выходных файлов (по умолчанию /app/output)
    S3_BUCKET        - S3 бакет (опционально)
    S3_ENDPOINT      - S3 эндпоинт (опционально)
    S3_ACCESS_KEY    - S3 ключ доступа (опционально)
    S3_SECRET_KEY    - S3 секретный ключ (опционально)
"""
import os
import sys
import csv
import logging
from datetime import datetime, date, timedelta
from pathlib import Path
from typing import Optional

import psycopg2
from psycopg2.extras import RealDictCursor

# ---------------------------------------------------------------------------
# Логирование
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S",
)
log = logging.getLogger("export")


# ---------------------------------------------------------------------------
# Конфигурация из переменных окружения
# ---------------------------------------------------------------------------
class Config:
    def __init__(self) -> None:
        self.db_host = os.getenv("DB_HOST", "localhost")
        self.db_port = int(os.getenv("DB_PORT", "5432"))
        self.db_name = os.getenv("DB_NAME", "logistics")
        self.db_user = os.getenv("DB_USER", "postgres")
        self.db_password = os.getenv("DB_PASSWORD", "postgres")

        self.export_table = os.getenv("EXPORT_TABLE", "shipments")
        self.export_date = date.today().strftime("%Y-%m-%d")
        self.output_dir = Path(os.getenv("OUTPUT_DIR", "/app/output"))

        # S3 (опционально)
        self.s3_bucket = os.getenv("S3_BUCKET", "")
        self.s3_endpoint = os.getenv("S3_ENDPOINT", "")
        self.s3_access_key = os.getenv("S3_ACCESS_KEY", "")
        self.s3_secret_key = os.getenv("S3_SECRET_KEY", "")

        # Валидация
        self._validate_date()

    def _validate_date(self) -> None:
        try:
            datetime.strptime(self.export_date, "%Y-%m-%d")
        except ValueError:
            log.error(f"Некорректный формат даты: {self.export_date}")
            sys.exit(1)


# ---------------------------------------------------------------------------
# Работа с БД
# ---------------------------------------------------------------------------
class Database:
    def __init__(self, config: Config) -> None:
        self.config = config
        self.conn: Optional[psycopg2.extensions.connection] = None

    def connect(self) -> None:
        log.info(
            "Подключение к PostgreSQL: %s:%s/%s",
            self.config.db_host,
            self.config.db_port,
            self.config.db_name,
        )
        self.conn = psycopg2.connect(
            host=self.config.db_host,
            port=self.config.db_port,
            dbname=self.config.db_name,
            user=self.config.db_user,
            password=self.config.db_password,
            connect_timeout=10,
        )
        self.conn.set_session(autocommit=True)

    def close(self) -> None:
        if self.conn:
            self.conn.close()

    def fetch_rows(self, table: str) -> list[dict]:
        """Извлекает строки за указанную дату."""
        query = f"""
            SELECT *
            FROM {table}
            ORDER BY id
        """
        with self.conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(query)
            rows = cur.fetchall()
        return rows


# ---------------------------------------------------------------------------
# Запись CSV
# ---------------------------------------------------------------------------
def write_csv(rows: list[dict], output_path: Path) -> int:
    """Записывает список словарей в CSV, возвращает размер файла."""
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        log.warning("Нет данных для записи")
        with open(output_path, "w", newline="") as fh:
            fh.write("")
        return 0

    with open(output_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)

    size_bytes = output_path.stat().st_size
    return size_bytes



# ---------------------------------------------------------------------------
# Главная логика
# ---------------------------------------------------------------------------
def main() -> None:
    start_time = datetime.now()
    log.info("=" * 60)
    log.info("Запуск экспорта данных грузоперевозок")
    log.info("=" * 60)

    # 1. Конфигурация
    config = Config()
    log.info("Дата экспорта: %s", config.export_date)
    log.info("Таблица: %s", config.export_table)

    # 2. Подключение к БД
    db = Database(config)
    try:
        db.connect()

        # Определяем колонку с датой (для фактовых таблиц)
        
        log.info(
                "Извлечение данных за %s из таблицы %s",
                config.export_date,
                config.export_table,
        )
        rows = db.fetch_rows(config.export_table)
        
        log.info("Извлечено строк: %d", len(rows))

        # 3. Запись в CSV
        output_file = config.output_dir / f"{config.export_table}_{config.export_date}.csv"
        size_bytes = write_csv(rows, output_file)
        log.info("CSV сохранён: %s (%.2f КБ)", output_file, size_bytes / 1024)


    except Exception as exc:
        log.exception("КРИТИЧЕСКАЯ ОШИБКА: %s", exc)
        sys.exit(1)
    finally:
        db.close()

    # 5. Итого
    elapsed = (datetime.now() - start_time).total_seconds()
    log.info("=" * 60)
    log.info("Экспорт завершён за %.2f секунд", elapsed)
    log.info("=" * 60)


if __name__ == "__main__":
    main()