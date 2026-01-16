from __future__ import annotations

import json
import os
from datetime import datetime, timedelta
from pathlib import Path

import pandas as pd
import requests

from airflow import DAG
from airflow.operators.python import PythonOperator, BranchPythonOperator
from airflow.operators.empty import EmptyOperator
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.providers.http.operators.http import SimpleHttpOperator
from airflow.operators.email import EmailOperator


DATA_DIR = Path("/opt/airflow/data")
OUT_DIR = Path("/opt/airflow/out")
TMP_DIR = Path("/opt/airflow/tmp")


def extract_csv(**context) -> dict:
    csv_path = DATA_DIR / "delivery_statuses.csv"
    df = pd.read_csv(csv_path)

    required_cols = {"order_id", "status", "updated_at"}
    if not required_cols.issubset(set(df.columns)):
        raise ValueError(f"CSV missing columns. Expected {required_cols}, got {set(df.columns)}")

    df["order_id"] = df["order_id"].astype(int)
    delayed = int((df["status"] == "DELAYED").sum())

    return {"csv_rows": int(len(df)), "csv_delayed": delayed}


def extract_postgres(**context) -> dict:
    hook = PostgresHook(postgres_conn_id="source_pg")
    sql = """
      SELECT
        o.order_id,
        o.created_at,
        o.total_amount,
        u.email AS user_email,
        u.city,
        p.status AS payment_status,
        p.amount AS payment_amount
      FROM orders o
      JOIN users u ON u.user_id = o.user_id
      LEFT JOIN LATERAL (
        SELECT status, amount
        FROM payments p
        WHERE p.order_id = o.order_id
        ORDER BY p.payment_id DESC
        LIMIT 1
      ) p ON true
      ORDER BY o.order_id;
    """
    records = hook.get_records(sql)
    cols = ["order_id", "created_at", "total_amount", "user_email", "city", "payment_status", "payment_amount"]
    df = pd.DataFrame(records, columns=cols)

    return {"db_rows": int(len(df))}


def merge_profile_and_write_report(**context) -> dict:
    ti = context["ti"]
    csv_stats = ti.xcom_pull(task_ids="extract_csv") or {}
    # db_stats = ti.xcom_pull(task_ids="extract_postgres") or {}

    deliveries = pd.read_csv(DATA_DIR / "delivery_statuses.csv")
    deliveries["order_id"] = deliveries["order_id"].astype(int)

    hook = PostgresHook(postgres_conn_id="source_pg")
    df_db = pd.read_sql(
        """
        SELECT
          o.order_id,
          o.created_at,
          o.total_amount,
          u.email AS user_email,
          u.city,
          COALESCE(p.status, 'UNKNOWN') AS payment_status,
          COALESCE(p.amount, 0) AS payment_amount
        FROM orders o
        JOIN users u ON u.user_id = o.user_id
        LEFT JOIN LATERAL (
          SELECT status, amount
          FROM payments p
          WHERE p.order_id = o.order_id
          ORDER BY p.payment_id DESC
          LIMIT 1
        ) p ON true
        ORDER BY o.order_id
        """,
        con=hook.get_sqlalchemy_engine(),
    )

    merged = df_db.merge(deliveries, on="order_id", how="left")

    total_orders = int(len(merged))
    delayed_orders = int((merged["status"] == "DELAYED").sum())
    paid_orders = int((merged["payment_status"] == "PAID").sum())
    failed_payments = int((merged["payment_status"] == "FAILED").sum())
    revenue_paid = float(merged.loc[merged["payment_status"] == "PAID", "payment_amount"].sum())

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    report_path = OUT_DIR / f"report_{context['ds']}.csv"
    merged.to_csv(report_path, index=False)

    return {
        "report_path": str(report_path),
        "total_orders": total_orders,
        "delayed_orders": delayed_orders,
        "paid_orders": paid_orders,
        "failed_payments": failed_payments,
        "revenue_paid": revenue_paid,
        "csv_rows": csv_stats.get("csv_rows", None),
        "csv_delayed": csv_stats.get("csv_delayed", None),
    }


def flaky_step_for_retry(**context) -> None:
    TMP_DIR.mkdir(parents=True, exist_ok=True)
    marker = TMP_DIR / f"retry_marker_{context['ds']}.flag"
    if not marker.exists():
        marker.write_text(f"created_at={datetime.utcnow().isoformat()}Z\n")
        raise RuntimeError("Intentional failure to demonstrate retries (marker created).")


def branch_on_delays(**context) -> str:
    ti = context["ti"]
    stats = ti.xcom_pull(task_ids="merge_profile_write_report") or {}
    if int(stats.get("delayed_orders", 0)) > 0:
        return "notify_delays"
    return "notify_ok"


default_args = {
    "owner": "marketing",
    "retries": 2,
    "retry_delay": timedelta(seconds=30),
    "email": ["demo-recipient@example.local"],
    "email_on_failure": True,
    "email_on_retry": True,
}

with DAG(
    dag_id="marketing_batch_poc",
    start_date=datetime(2026, 1, 15),
    schedule="@daily",
    catchup=False,
    default_args=default_args,
    tags=["poc", "batch", "marketing"],
) as dag:

    start = EmptyOperator(task_id="start")

    t_extract_csv = PythonOperator(
        task_id="extract_csv",
        python_callable=extract_csv,
    )

    t_extract_pg = PythonOperator(
        task_id="extract_postgres",
        python_callable=extract_postgres,
    )

    t_merge = PythonOperator(
        task_id="merge_profile_write_report",
        python_callable=merge_profile_and_write_report,
    )

    t_call_api = SimpleHttpOperator(
        task_id="call_external_api",
        http_conn_id="echo_api",
        endpoint="/",
        method="GET",
        log_response=True,
    )

    t_flaky = PythonOperator(
        task_id="flaky_step_retry_demo",
        python_callable=flaky_step_for_retry,
        retries=2,
        retry_delay=timedelta(seconds=15),
    )

    t_branch = BranchPythonOperator(
        task_id="branch_on_delays",
        python_callable=branch_on_delays,
    )

    notify_delays = EmailOperator(
        task_id="notify_delays",
        to=["ops@example.local"],
        subject="[POC] Delayed deliveries detected ({{ ds }})",
        html_content="""
        <h3>Delayed deliveries found</h3>
        <p>See attached report path in logs / XCom.</p>
        <p>DAG: {{ dag.dag_id }}, Run: {{ run_id }}</p>
        """,
    )

    notify_ok = EmailOperator(
        task_id="notify_ok",
        to=["ops@example.local"],
        subject="[POC] Batch OK ({{ ds }})",
        html_content="""
        <h3>No delayed deliveries</h3>
        <p>DAG: {{ dag.dag_id }}, Run: {{ run_id }}</p>
        """,
    )

    join = EmptyOperator(task_id="join", trigger_rule="none_failed_min_one_success")
    end = EmptyOperator(task_id="end")

    start >> [t_extract_csv, t_extract_pg] >> t_merge >> t_call_api >> t_flaky >> t_branch
    t_branch >> [notify_delays, notify_ok] >> join >> end
