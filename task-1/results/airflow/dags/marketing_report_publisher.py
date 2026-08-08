"""
POC Task 1 — downstream-контур, запускаемый СОБЫТИЕМ.

Демонстрирует event-trigger: DAG не имеет
расписания — он запускается автоматически, когда DAG
`marketing_data_pipeline` обновляет Asset `marketing://orders_mart`.

Также демонстрирует dynamic task mapping: «публикация» каждого отчёта
выполняется отдельным параллельным экземпляром задачи.
"""

from datetime import datetime, timedelta
import glob
import json
import logging
import os

from airflow.providers.smtp.notifications.smtp import send_smtp_notification
from airflow.sdk import Asset, dag, task

RESULTS_DIR = "/opt/airflow/results"

ORDERS_MART = Asset("marketing://orders_mart")


@dag(
    dag_id="marketing_report_publisher",
    description="POC: event-trigger по Asset — публикация отчётов во внешние системы",
    start_date=datetime(2026, 7, 1),
    schedule=[ORDERS_MART],  # <-- event-trigger: запуск по событию обновления Asset
    catchup=False,
    tags=["marketing", "task1", "poc", "event-driven"],
    default_args={
        "owner": "marketing_data_team",
        "retries": 2,
        "retry_delay": timedelta(seconds=30),
        "email": ["marketing-alerts@company.local"],
        "email_on_failure": True,
    },
)
def marketing_report_publisher():

    @task
    def collect_reports() -> list:
        """Собрать список готовых отчётов из витрины результатов."""
        reports = sorted(
            p for p in glob.glob(os.path.join(RESULTS_DIR, "*.csv"))
            if "/stg_" not in p and "/quarantine/" not in p
        )
        logging.info("Найдено отчётов для публикации: %s", len(reports))
        return reports

    @task
    def publish_report(report_path: str) -> dict:
        """
        Имитация публикации одного отчёта во внешние системы.
        В боевом контуре здесь — GCSToBigQueryOperator / S3ToRedshiftOperator /
        KafkaProduceOperator; в POC пишем маркер публикации в файл.
        """
        out_dir = os.path.join(RESULTS_DIR, "published")
        os.makedirs(out_dir, exist_ok=True)
        marker = os.path.join(out_dir, os.path.basename(report_path) + ".published.json")
        with open(marker, "w") as f:
            json.dump(
                {
                    "report": report_path,
                    "targets": ["bigquery", "redshift", "kafka"],
                    "published_at": datetime.now().isoformat(),
                },
                f, indent=2,
            )
        logging.info("Опубликован отчёт %s -> %s", report_path, marker)
        return {"report": report_path, "marker": marker}

    @task(
        # Email об успешной публикации «из коробки»
        on_success_callback=send_smtp_notification(
            from_email="airflow@marketing-poc.local",
            to="marketing-alerts@company.local",
            subject="[OK] Marketing Report Publisher: отчёты опубликованы",
            html_content="Event-triggered DAG marketing_report_publisher успешно "
                         "опубликовал все отчёты во внешние системы.",
        ),
    )
    def notify_publish_done(published: list) -> str:
        """Финальное уведомление об успешной публикации (email «из коробки»)."""
        logging.info("Публикация завершена, отчётов: %s", len(published))
        return f"published {len(published)} reports"

    reports = collect_reports()
    # Dynamic task mapping: по экземпляру задачи на каждый отчёт
    published = publish_report.expand(report_path=reports)
    notify_publish_done(published)


marketing_report_publisher()
