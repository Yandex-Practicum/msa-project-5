"""
TradeWare: DAG пакетной обработки складских отчетов.

Демонстрирует:
1. Чтение из источника данных (CSV-файл + PostgreSQL)
2. Ветвление пайплайна на основе анализа данных
3. Email-уведомления при успехе и ошибке
4. Retry-политику с экспоненциальным backoff
"""

from __future__ import annotations

import csv
import logging
import os
from datetime import datetime, timedelta
from typing import Any

from airflow import DAG
from airflow.operators.email import EmailOperator
from airflow.operators.empty import EmptyOperator
from airflow.operators.python import BranchPythonOperator, PythonOperator
from airflow.providers.postgres.hooks.postgres import PostgresHook

logger: logging.Logger = logging.getLogger(__name__)

DATA_DIR: str = "/opt/airflow/data"
CSV_FILE: str = os.path.join(DATA_DIR, "sample_report.csv")

# Порог для ветвления: файлы с количеством строк выше порога
# обрабатываются пакетно, ниже - построчно
BATCH_THRESHOLD: int = 20


# Коллбэки для уведомлений


def on_success_callback(context: dict[str, Any]) -> None:
    """Успешное завершение DAG."""
    dag_id: str = context["dag"].dag_id
    execution_date: datetime = context["execution_date"]
    logger.info(
        "DAG '%s' завершен успешно (execution_date=%s). " "В продакшене здесь будет отправка email/Slack уведомления.",
        dag_id,
        execution_date,
    )


def on_failure_callback(context: dict[str, Any]) -> None:
    """Ошибка в задаче DAG."""
    task_id: str = context["task_instance"].task_id
    dag_id: str = context["dag"].dag_id
    exception: Any = context.get("exception", "N/A")
    logger.error(
        "Ошибка в задаче '%s' DAG '%s': %s. " "В продакшене здесь будет отправка email/Slack уведомления.",
        task_id,
        dag_id,
        exception,
    )


# Задачи пайплайна


def read_and_validate_csv(**context: Any) -> int:
    """
    Шаг 1: Чтение CSV-файла и валидация формата.

    Возвращает количество строк и данные через XCom.
    """
    logger.info("Чтение файла: %s", CSV_FILE)

    if not os.path.exists(CSV_FILE):
        raise FileNotFoundError(f"Файл не найден: {CSV_FILE}")

    rows: list[dict[str, Any]] = []
    required_columns: set[str] = {"sku", "warehouse_code", "quantity", "report_date"}

    with open(CSV_FILE, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)

        # Валидация заголовков
        if not required_columns.issubset(set(reader.fieldnames or [])):
            missing = required_columns - set(reader.fieldnames or [])
            raise ValueError(f"Ошибка формата загружаемых данных. " f"Отсутствуют колонки: {missing}")

        for i, row in enumerate(reader, start=1):
            # Валидация данных строки
            if not row["sku"] or not row["warehouse_code"]:
                logger.warning("Строка %d: пустой SKU или код склада, пропуск", i)
                continue
            try:
                row["quantity"] = int(row["quantity"])
            except (ValueError, TypeError):
                logger.warning("Строка %d: некорректное количество '%s', пропуск", i, row.get("quantity"))
                continue
            rows.append(row)

    row_count: int = len(rows)
    logger.info("Валидация завершена: %d корректных строк из файла", row_count)

    # Передаем данные через XCom
    context["ti"].xcom_push(key="row_count", value=row_count)
    context["ti"].xcom_push(key="csv_data", value=rows)

    return row_count


def decide_processing_branch(**context: Any) -> str:
    """
    Шаг 2: Ветвление - выбор способа обработки на основе объема данных.

    - Больше BATCH_THRESHOLD строк -> пакетная обработка (batch)
    - Меньше или равно -> построчная обработка (online)
    """
    row_count: int = context["ti"].xcom_pull(task_ids="read_and_validate_csv", key="row_count")
    logger.info("Количество строк: %d, порог: %d", row_count, BATCH_THRESHOLD)

    if row_count > BATCH_THRESHOLD:
        logger.info("Выбрана ПАКЕТНАЯ обработка (batch)")
        return "batch_process"
    else:
        logger.info("Выбрана ПОСТРОЧНАЯ обработка (online)")
        return "online_process"


def batch_process(**context: Any) -> int:
    """
    Шаг 3а: Пакетная обработка - загрузка всех строк одним INSERT.

    Используется для больших файлов (>BATCH_THRESHOLD строк).
    """
    csv_data: list[dict[str, Any]] = context["ti"].xcom_pull(task_ids="read_and_validate_csv", key="csv_data")
    hook: PostgresHook = PostgresHook(postgres_conn_id="tradeware_db")

    logger.info("Пакетная загрузка %d строк", len(csv_data))

    # Обогащение данными из справочника и пакетная вставка
    insert_sql: str = """
        INSERT INTO warehouse_inventory
            (sku, warehouse_code, quantity, report_date,
             product_name, category, price, promo_discount)
        SELECT
            %(sku)s, %(warehouse_code)s, %(quantity)s, %(report_date)s::date,
            pr.product_name, pr.category, pr.price, pr.promo_discount
        FROM product_reference pr
        WHERE pr.sku = %(sku)s
        ON CONFLICT (sku, warehouse_code, report_date)
        DO UPDATE SET
            quantity = EXCLUDED.quantity,
            product_name = EXCLUDED.product_name,
            category = EXCLUDED.category,
            price = EXCLUDED.price,
            promo_discount = EXCLUDED.promo_discount,
            loaded_at = NOW()
    """

    conn: Any = hook.get_conn()
    cursor: Any = conn.cursor()
    loaded: int = 0
    try:
        for row in csv_data:
            cursor.execute(insert_sql, row)
            loaded += 1
        conn.commit()
    finally:
        cursor.close()
        conn.close()

    logger.info("Пакетная обработка завершена: загружено %d строк", loaded)
    context["ti"].xcom_push(key="rows_loaded", value=loaded)
    return loaded


def online_process(**context: Any) -> int:
    """
    Шаг 3б: Построчная обработка - загрузка каждой строки отдельно с детальным логированием.

    Используется для малых файлов.
    """
    csv_data: list[dict[str, Any]] = context["ti"].xcom_pull(task_ids="read_and_validate_csv", key="csv_data")
    hook: PostgresHook = PostgresHook(postgres_conn_id="tradeware_db")

    logger.info("Построчная обработка %d строк", len(csv_data))

    insert_sql: str = """
        INSERT INTO warehouse_inventory
            (sku, warehouse_code, quantity, report_date,
             product_name, category, price, promo_discount)
        SELECT
            %(sku)s, %(warehouse_code)s, %(quantity)s, %(report_date)s::date,
            pr.product_name, pr.category, pr.price, pr.promo_discount
        FROM product_reference pr
        WHERE pr.sku = %(sku)s
        ON CONFLICT (sku, warehouse_code, report_date)
        DO UPDATE SET
            quantity = EXCLUDED.quantity,
            product_name = EXCLUDED.product_name,
            category = EXCLUDED.category,
            price = EXCLUDED.price,
            promo_discount = EXCLUDED.promo_discount,
            loaded_at = NOW()
    """

    loaded: int = 0
    errors: int = 0
    conn: Any = hook.get_conn()
    cursor: Any = conn.cursor()
    try:
        for i, row in enumerate(csv_data, start=1):
            try:
                cursor.execute(insert_sql, row)
                conn.commit()
                loaded += 1
                logger.info("Строка %d (SKU=%s, склад=%s): OK", i, row["sku"], row["warehouse_code"])
            except Exception as e:
                conn.rollback()
                errors += 1
                logger.error("Строка %d: ошибка - %s", i, e)
    finally:
        cursor.close()
        conn.close()

    logger.info("Построчная обработка завершена: загружено %d, ошибок %d", loaded, errors)
    context["ti"].xcom_push(key="rows_loaded", value=loaded)
    return loaded


def log_load_result(**context: Any) -> None:
    """Шаг 4: Запись результата загрузки в лог-таблицу."""
    row_count: int = context["ti"].xcom_pull(task_ids="read_and_validate_csv", key="row_count") or 0
    # Пробуем получить результат из обеих веток
    rows_loaded: int = (
        context["ti"].xcom_pull(task_ids="batch_process", key="rows_loaded")
        or context["ti"].xcom_pull(task_ids="online_process", key="rows_loaded")
        or 0
    )

    hook: PostgresHook = PostgresHook(postgres_conn_id="tradeware_db")

    rows_errors: int = row_count - rows_loaded
    status: str = "SUCCESS" if rows_loaded == row_count else "PARTIAL"
    hook.run(
        """
        INSERT INTO load_log (filename, rows_total, rows_loaded, rows_errors, status, started_at)
        VALUES (%s, %s, %s, %s, %s, %s)
        """,
        parameters=(
            CSV_FILE,
            row_count,
            rows_loaded,
            rows_errors,
            status,
            str(context["execution_date"]),
        ),
    )
    logger.info(
        "Результат загрузки записан: total=%d, loaded=%d, status=%s",
        row_count,
        rows_loaded,
        status,
    )


# Определение DAG

default_args: dict[str, Any] = {
    "owner": "tradeware",
    "depends_on_past": False,
    # Retry-политика
    "retries": 3,
    "retry_delay": timedelta(seconds=30),
    "retry_exponential_backoff": True,
    "max_retry_delay": timedelta(minutes=10),
    # Email-уведомления
    "email": ["ops@tradeware.local"],
    "email_on_failure": True,
    "email_on_retry": True,
    # Callbacks
    "on_failure_callback": on_failure_callback,
}

with DAG(
    dag_id="tradeware_warehouse_report",
    default_args=default_args,
    description="Обработка складских отчетов TradeWare: валидация, обогащение, загрузка",
    schedule=None,  # Запуск вручную (в проде - по расписанию или триггеру)
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["tradeware", "warehouse", "etl"],
    on_success_callback=on_success_callback,
) as dag:

    # Шаг 1: Чтение и валидация CSV
    read_csv = PythonOperator(
        task_id="read_and_validate_csv",
        python_callable=read_and_validate_csv,
    )

    # Шаг 2: Ветвление по объему данных
    branch = BranchPythonOperator(
        task_id="decide_processing_branch",
        python_callable=decide_processing_branch,
    )

    # Шаг 3а: Пакетная обработка (для больших файлов)
    batch = PythonOperator(
        task_id="batch_process",
        python_callable=batch_process,
    )

    # Шаг 3б: Построчная обработка (для малых файлов)
    online = PythonOperator(
        task_id="online_process",
        python_callable=online_process,
    )

    # Точка объединения веток
    join = EmptyOperator(
        task_id="join_branches",
        trigger_rule="none_failed_min_one_success",
    )

    # Шаг 4: Логирование результата
    log_result = PythonOperator(
        task_id="log_load_result",
        python_callable=log_load_result,
        trigger_rule="none_failed_min_one_success",
    )

    # Шаг 5: Email при успехе
    notify_success = EmailOperator(
        task_id="notify_success",
        to="ops@tradeware.local",
        subject="[TradeWare] Отчет успешно загружен",
        html_content="""
        <h3>Загрузка складского отчета завершена успешно</h3>
        <p>DAG: {{ dag.dag_id }}</p>
        <p>Дата выполнения: {{ ds }}</p>
        """,
        trigger_rule="none_failed_min_one_success",
    )

    # Шаг 6: Email при ошибке (fallback)
    notify_failure = EmailOperator(
        task_id="notify_failure",
        to="ops@tradeware.local",
        subject="[TradeWare] ОШИБКА загрузки отчета",
        html_content="""
        <h3>Ошибка при загрузке складского отчета</h3>
        <p>DAG: {{ dag.dag_id }}</p>
        <p>Дата выполнения: {{ ds }}</p>
        <p>Проверьте логи в Airflow UI.</p>
        """,
        trigger_rule="one_failed",
    )

    # Граф зависимостей
    read_csv >> branch
    branch >> [batch, online]
    [batch, online] >> join
    join >> log_result
    log_result >> [notify_success, notify_failure]
