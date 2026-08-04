"""
POC Task 1 — пайплайн пакетной обработки маркетинговых данных.

Демонстрирует:
  * чтение из двух источников: PostgreSQL (заказы+платежи+пользователи) и CSV (статусы доставок);
  * объединение и анализ данных;
  * ветвление пайплайна по условию (@task.branch);
  * retry-политику с экспоненциальным backoff;
  * fallback-логику (quarantine через trigger_rule=ONE_FAILED);
  * email-уведомления «из коробки» (email_on_failure/email_on_retry/email_on_success, SMTP -> MailHog);
  * event-trigger: публикация Asset `marketing_orders_mart`, на который подписан
    DAG `marketing_report_publisher` (data-aware scheduling).

Сценарий отказа воспроизводится детерминированно: при запуске DAG
передайте конфигурацию {"simulate_failure": true}.
"""

from datetime import datetime, timedelta
import json
import logging
import os

import pandas as pd

from airflow.sdk import Asset, Param, dag, get_current_context, task
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.providers.smtp.notifications.smtp import send_smtp_notification
from airflow.providers.standard.operators.empty import EmptyOperator
from airflow.task.trigger_rule import TriggerRule

RESULTS_DIR = "/opt/airflow/results"
SAMPLE_DATA_DIR = "/opt/airflow/sample_data"

# Asset, обновление которого является событием-триггером для DAG marketing_report_publisher
ORDERS_MART = Asset("marketing://orders_mart")

# Доля проблемных заказов, выше которой пайплайн уходит в ветку расследования аномалий
PROBLEM_RATE_THRESHOLD = 0.25

default_args = {
    "owner": "marketing_data_team",
    "depends_on_past": False,
    # Email-уведомления «из коробки»: Airflow сам шлёт письма через SMTP (MailHog)
    "email": ["marketing-alerts@company.local"],
    "email_on_failure": True,
    "email_on_retry": True,
    # Базовая retry-политика для всех задач
    "retries": 1,
    "retry_delay": timedelta(seconds=30),
}


@dag(
    dag_id="marketing_data_pipeline",
    description="POC: консолидация заказов/платежей/доставок, анализ, ветвление, отчёты",
    default_args=default_args,
    start_date=datetime(2026, 7, 1),
    schedule=None,  # ручной запуск для POC; в бою — cron или по событиям
    catchup=False,
    tags=["marketing", "task1", "poc"],
    params={
        # Детерминированный переключатель failure-сценария (вместо random в примере)
        "simulate_failure": Param(
            False,
            type="boolean",
            description="Симулировать сбой объединения источников (для демонстрации retry/fallback/email)",
        ),
    },
)
def marketing_data_pipeline():

    start = EmptyOperator(task_id="start_pipeline")

    @task(
        retries=3,
        retry_delay=timedelta(seconds=30),
        retry_exponential_backoff=True,   # 30s -> 60s -> 120s
        max_retry_delay=timedelta(minutes=5),
    )
    def read_orders_payments() -> str:
        """Чтение заказов, платежей и пользователей из PostgreSQL (источник №1)."""
        hook = PostgresHook(postgres_conn_id="marketing_db")
        df = hook.get_pandas_df(
            """
            SELECT o.order_id, o.user_id, o.product_category, o.amount, o.created_at,
                   p.payment_status, u.segment, u.city
            FROM orders o
            JOIN payments p ON p.order_id = o.order_id
            JOIN users u    ON u.user_id  = o.user_id
            """
        )
        path = os.path.join(RESULTS_DIR, "stg_orders_payments.parquet")
        df.to_parquet(path, index=False)
        logging.info("PostgreSQL: прочитано %s заказов", len(df))
        return path

    @task(
        retries=3,
        retry_delay=timedelta(seconds=30),
        retry_exponential_backoff=True,
        max_retry_delay=timedelta(minutes=5),
    )
    def read_delivery_statuses() -> str:
        """Чтение статусов доставок из CSV-файла (источник №2, файловое хранилище)."""
        df = pd.read_csv(os.path.join(SAMPLE_DATA_DIR, "delivery_statuses.csv"))
        path = os.path.join(RESULTS_DIR, "stg_delivery_statuses.parquet")
        df.to_parquet(path, index=False)
        logging.info("CSV: прочитано %s статусов доставок", len(df))
        return path

    @task(retries=2, retry_delay=timedelta(seconds=30))
    def merge_sources(orders_path: str, delivery_path: str) -> str:
        """Объединение источников в единую витрину заказов."""
        ctx = get_current_context()
        if ctx["params"]["simulate_failure"]:
            raise RuntimeError(
                "Симуляция сбоя объединения источников (включено параметром simulate_failure)"
            )

        orders = pd.read_parquet(orders_path)
        delivery = pd.read_parquet(delivery_path)
        mart = orders.merge(delivery[["order_id", "delivery_status"]],
                            on="order_id", how="left")
        mart["is_problem"] = (mart["payment_status"] != "paid") | (
            mart["delivery_status"].isin(["lost", "returned"])
        )
        path = os.path.join(RESULTS_DIR, "orders_mart.csv")
        mart.to_csv(path, index=False)
        logging.info("Витрина собрана: %s записей -> %s", len(mart), path)
        return path

    @task(retries=2, retry_delay=timedelta(seconds=30))
    def analyze_orders(mart_path: str) -> dict:
        """Анализ витрины: доля проблемных заказов, выручка по категориям."""
        mart = pd.read_csv(mart_path)
        total = len(mart)
        problem = int(mart["is_problem"].sum())
        analysis = {
            "analysis_ts": datetime.now().isoformat(),
            "total_orders": total,
            "problem_orders": problem,
            "problem_rate": problem / total if total else 0.0,
            "paid_revenue": float(mart.loc[mart["payment_status"] == "paid", "amount"].sum()),
            "revenue_by_category": mart.groupby("product_category")["amount"]
            .sum().round(2).to_dict(),
            "threshold": PROBLEM_RATE_THRESHOLD,
        }
        with open(os.path.join(RESULTS_DIR, "analysis.json"), "w") as f:
            json.dump(analysis, f, indent=2, ensure_ascii=False)
        logging.info(
            "Анализ: всего %s, проблемных %s (%.1f%%), порог %.1f%%",
            total, problem, analysis["problem_rate"] * 100, PROBLEM_RATE_THRESHOLD * 100,
        )
        return analysis

    @task.branch
    def route_by_health(analysis: dict) -> str:
        """Ветвление: норма -> отчёты по категориям; аномалия -> расследование."""
        if analysis["problem_rate"] > PROBLEM_RATE_THRESHOLD:
            logging.warning(
                "Доля проблемных заказов %.1f%% выше порога %.1f%% -> ветка аномалий",
                analysis["problem_rate"] * 100, PROBLEM_RATE_THRESHOLD * 100,
            )
            return "build_anomaly_report"
        logging.info(
            "Доля проблемных заказов %.1f%% в норме -> штатные отчёты",
            analysis["problem_rate"] * 100,
        )
        return "build_category_reports"

    @task(retries=2, retry_delay=timedelta(seconds=30))
    def build_category_reports(mart_path: str) -> list:
        """Штатная ветка: отдельный отчёт по каждой категории товаров."""
        mart = pd.read_csv(mart_path)
        files = []
        for category, grp in mart.groupby("product_category"):
            path = os.path.join(RESULTS_DIR, f"report_category_{category}.csv")
            grp.to_csv(path, index=False)
            files.append(path)
            logging.info("Отчёт по категории %s: %s заказов -> %s", category, len(grp), path)
        return files

    @task(retries=2, retry_delay=timedelta(seconds=30))
    def build_anomaly_report(mart_path: str) -> list:
        """Ветка аномалий: детальный отчёт по проблемным заказам."""
        mart = pd.read_csv(mart_path)
        problems = mart[mart["is_problem"]]
        path = os.path.join(RESULTS_DIR, "report_anomalies.csv")
        problems.to_csv(path, index=False)
        logging.warning("Отчёт по аномалиям: %s проблемных заказов -> %s", len(problems), path)
        return [path]

    @task(
        trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS,
        outlets=[ORDERS_MART],  # событие-триггер для DAG marketing_report_publisher
        # Email об успешном завершении пайплайна «из коробки» (notifier-фреймворк)
        on_success_callback=send_smtp_notification(
            from_email="airflow@marketing-poc.local",
            to="marketing-alerts@company.local",
            subject="[OK] Marketing Data Pipeline: пайплайн успешно завершён",
            html_content="Пайплайн marketing_data_pipeline завершился успехом. "
                         "Витрина и executive summary обновлены, отчёты опубликованы.",
        ),
    )
    def create_executive_summary(analysis: dict, mart_path: str) -> str:
        """Итоговая сводка; обновляет Asset, чем запускает downstream-контур."""
        summary = {
            "pipeline": "marketing_data_pipeline",
            "generated_at": datetime.now().isoformat(),
            "key_metrics": analysis,
            "mart": mart_path,
        }
        path = os.path.join(RESULTS_DIR, "executive_summary.json")
        with open(path, "w") as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        logging.info("Executive summary -> %s; asset %s обновлён", path, ORDERS_MART.uri)
        return path

    @task(trigger_rule=TriggerRule.ONE_FAILED)
    def quarantine_failed_batch() -> str:
        """Fallback-логика: при сбое любого шага подготовки данных — карантин батча."""
        os.makedirs(os.path.join(RESULTS_DIR, "quarantine"), exist_ok=True)
        marker = os.path.join(
            RESULTS_DIR, "quarantine", f"failed_batch_{datetime.now():%Y%m%d_%H%M%S}.json"
        )
        with open(marker, "w") as f:
            json.dump(
                {
                    "reason": "one of the upstream tasks failed",
                    "action": "batch quarantined, manual investigation required",
                },
                f, indent=2,
            )
        logging.error("Батч отправлен в карантин: %s", marker)
        return marker

    end = EmptyOperator(
        task_id="end_pipeline",
        trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS,
    )

    orders_stg = read_orders_payments()
    delivery_stg = read_delivery_statuses()
    mart = merge_sources(orders_stg, delivery_stg)
    analysis = analyze_orders(mart)
    route = route_by_health(analysis)

    category_reports = build_category_reports(mart)
    anomaly_report = build_anomaly_report(mart)
    route >> [category_reports, anomaly_report]

    summary = create_executive_summary(analysis, mart)
    [category_reports, anomaly_report] >> summary >> end

    # Fallback: сбой чтения или объединения -> карантин (skipped при успехе)
    quarantine = quarantine_failed_batch()
    [orders_stg, delivery_stg, mart] >> quarantine

    start >> [orders_stg, delivery_stg]


marketing_data_pipeline()
