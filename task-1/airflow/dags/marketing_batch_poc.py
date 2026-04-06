from __future__ import annotations

import csv
import json
import os
from datetime import datetime, timedelta
from pathlib import Path

from airflow.decorators import dag, task
from airflow.exceptions import AirflowException, AirflowFailException
from airflow.operators.email import EmailOperator
from airflow.operators.empty import EmptyOperator
from airflow.operators.python import get_current_context
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.utils.trigger_rule import TriggerRule

INPUT_DIR = Path("/opt/airflow/include")
OUTPUT_DIR = Path("/opt/airflow/output")
CSV_PATH = INPUT_DIR / "delivery_statuses.csv"
REPORT_PATH = OUTPUT_DIR / "latest_report.json"
PUBLISHED_REPORT_PATH = OUTPUT_DIR / "published_report.json"
INCIDENT_PATH = OUTPUT_DIR / "incident_alert.json"


def _as_bool(value: str, default: bool) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dag(
    dag_id="marketing_batch_poc",
    description="Batch processing POC with PostgreSQL, CSV, branching, retry, and email notifications.",
    start_date=datetime(2025, 1, 1),
    schedule="@daily",
    catchup=False,
    default_args={
        "owner": "marketing-platform",
        "email": ["marketing-ops@example.com"],
        "email_on_failure": True,
        "email_on_retry": True,
        "retries": 2,
        "retry_delay": timedelta(seconds=20),
    },
    tags=["task-1", "batch", "poc"],
)
def marketing_batch_poc():
    start = EmptyOperator(task_id="start")
    finish = EmptyOperator(
        task_id="finish",
        trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS,
    )

    @task(task_id="extract_orders_and_payments")
    def extract_orders_and_payments() -> list[dict]:
        hook = PostgresHook(postgres_conn_id="demo_postgres")
        rows = hook.get_records(
            """
            SELECT
                o.order_id,
                o.customer_email,
                o.customer_segment,
                o.total_amount,
                p.payment_status
            FROM orders AS o
            LEFT JOIN payments AS p
                ON p.order_id = o.order_id
            ORDER BY o.order_id
            """
        )

        return [
            {
                "order_id": row[0],
                "customer_email": row[1],
                "customer_segment": row[2],
                "total_amount": float(row[3]),
                "payment_status": row[4] or "unknown",
            }
            for row in rows
        ]

    @task(task_id="read_delivery_statuses")
    def read_delivery_statuses() -> list[dict]:
        with CSV_PATH.open("r", encoding="utf-8", newline="") as source:
            reader = csv.DictReader(source)
            return list(reader)

    @task(
        task_id="build_customer_snapshot",
        retries=2,
        retry_delay=timedelta(seconds=15),
    )
    def build_customer_snapshot(db_rows: list[dict], delivery_rows: list[dict]) -> dict:
        context = get_current_context()
        simulate_transient_failure = _as_bool(
            os.getenv("SIMULATE_TRANSIENT_FAILURE"),
            default=True,
        )

        if simulate_transient_failure and context["ti"].try_number == 1:
            raise AirflowException(
                "Simulated transient enrichment failure to demonstrate retry handling."
            )

        delivery_by_order = {
            row["order_id"]: {
                "delivery_status": row["delivery_status"],
                "carrier": row["carrier"],
            }
            for row in delivery_rows
        }

        delayed_or_failed = 0
        missing_delivery_status = 0
        paid_orders = 0
        enriched_rows = []

        for order in db_rows:
            delivery = delivery_by_order.get(order["order_id"])
            delivery_status = "missing"
            carrier = "n/a"

            if delivery:
                delivery_status = delivery["delivery_status"]
                carrier = delivery["carrier"]
            else:
                missing_delivery_status += 1

            if delivery_status in {"delayed", "failed"}:
                delayed_or_failed += 1

            if order["payment_status"] == "paid":
                paid_orders += 1

            enriched_rows.append(
                {
                    **order,
                    "delivery_status": delivery_status,
                    "carrier": carrier,
                }
            )

        summary = {
            "generated_at": datetime.utcnow().isoformat() + "Z",
            "total_orders": len(db_rows),
            "paid_orders": paid_orders,
            "missing_delivery_status": missing_delivery_status,
            "delayed_or_failed_orders": delayed_or_failed,
            "rows": enriched_rows,
        }

        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(
            json.dumps(summary, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )

        return summary

    @task(task_id="quality_gate")
    def quality_gate(summary: dict) -> dict:
        max_missing = int(os.getenv("QUALITY_GATE_MAX_MISSING", "99"))
        if summary["missing_delivery_status"] > max_missing:
            raise AirflowFailException(
                f"Quality gate failed: missing delivery statuses = "
                f"{summary['missing_delivery_status']}, limit = {max_missing}."
            )
        return summary

    @task.branch(task_id="choose_route")
    def choose_route(summary: dict) -> str:
        threshold = int(os.getenv("ALERT_THRESHOLD", "2"))
        if summary["delayed_or_failed_orders"] >= threshold:
            return "escalate_delivery_incident"
        return "publish_standard_report"

    @task(task_id="publish_standard_report")
    def publish_standard_report(summary: dict) -> str:
        report = {
            "status": "published",
            "audience": "marketing-team",
            "summary": {
                "total_orders": summary["total_orders"],
                "paid_orders": summary["paid_orders"],
                "delayed_or_failed_orders": summary["delayed_or_failed_orders"],
            },
        }
        PUBLISHED_REPORT_PATH.write_text(
            json.dumps(report, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
        return "standard-report-published"

    @task(task_id="escalate_delivery_incident")
    def escalate_delivery_incident(summary: dict) -> str:
        incident = {
            "status": "incident-opened",
            "reason": "delivery_threshold_reached",
            "delayed_or_failed_orders": summary["delayed_or_failed_orders"],
            "recommended_action": "review_carrier_performance_and_notify_ops",
        }
        INCIDENT_PATH.write_text(
            json.dumps(incident, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
        return "incident-created"

    send_success_email = EmailOperator(
        task_id="send_success_email",
        to="marketing-ops@example.com",
        subject="Airflow POC: marketing batch completed successfully",
        html_content="""
        <h3>Marketing batch completed</h3>
        <p>DAG: marketing_batch_poc</p>
        <p>Delayed or failed orders:
        {{ ti.xcom_pull(task_ids='build_customer_snapshot')['delayed_or_failed_orders'] }}</p>
        <p>Output report: /opt/airflow/output/latest_report.json</p>
        """,
        trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS,
    )

    db_rows = extract_orders_and_payments()
    delivery_rows = read_delivery_statuses()
    summary = build_customer_snapshot(db_rows, delivery_rows)
    checked_summary = quality_gate(summary)
    route = choose_route(checked_summary)

    standard_report = publish_standard_report(checked_summary)
    incident_report = escalate_delivery_incident(checked_summary)

    start >> [db_rows, delivery_rows]
    [db_rows, delivery_rows] >> summary >> checked_summary >> route
    route >> [standard_report, incident_report] >> send_success_email >> finish


marketing_batch_poc()
