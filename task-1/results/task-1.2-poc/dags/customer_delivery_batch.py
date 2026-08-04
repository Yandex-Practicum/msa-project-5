import csv
import json
import re
from collections import Counter
from datetime import datetime, timedelta, timezone
from pathlib import Path

from airflow.exceptions import AirflowException
from airflow.sdk import dag, get_current_context, task
from airflow.utils.email import send_email

INPUT_FILE = Path("/opt/airflow/data/input/customer-deliveries.csv")
OUTPUT_DIR = Path("/opt/airflow/data/output")
EMAIL_TO = "student@example.local"
REQUIRED_COLUMNS = {
    "customer_id",
    "order_id",
    "delivery_status",
    "delivery_cost",
}


def _safe_run_id(context):
    return re.sub(r"[^a-zA-Z0-9_.-]", "_", context["run_id"])


def _send_status_email(context, status):
    dag_id = context["dag"].dag_id
    run_id = context["run_id"]
    send_email(
        to=EMAIL_TO,
        subject=f"[{status}] Airflow DAG {dag_id}",
        html_content=f"<p>DAG: <b>{dag_id}</b></p><p>Run: {run_id}</p>",
    )


def notify_success(context):
    _send_status_email(context, "SUCCESS")


def notify_failure(context):
    _send_status_email(context, "FAILED")


def notify_retry(context):
    _send_status_email(context, "RETRY")


@dag(
    dag_id="customer_delivery_batch",
    description="Task 1 POC: CSV analysis, branching, retry and email notifications",
    schedule=None,
    start_date=datetime(2025, 1, 1, tzinfo=timezone.utc),
    catchup=False,
    params={
        "force_failure": False,
        "large_batch_threshold": 5,
    },
    on_success_callback=notify_success,
    on_failure_callback=notify_failure,
    tags=["task-1", "poc"],
)
def customer_delivery_batch():
    @task
    def read_and_analyze_csv():
        context = get_current_context()
        if context["params"].get("force_failure"):
            raise AirflowException("Test failure requested by the DAG parameter")

        if not INPUT_FILE.exists():
            raise AirflowException(f"Input file is missing: {INPUT_FILE}")

        with INPUT_FILE.open(encoding="utf-8", newline="") as source:
            reader = csv.DictReader(source)
            missing_columns = REQUIRED_COLUMNS - set(reader.fieldnames or [])
            if missing_columns:
                raise AirflowException(
                    f"Required columns are missing: {sorted(missing_columns)}"
                )
            rows = list(reader)

        if not rows:
            raise AirflowException("Input CSV is empty")

        statuses = Counter(row["delivery_status"] for row in rows)
        total_cost = sum(float(row["delivery_cost"]) for row in rows)
        return {
            "row_count": len(rows),
            "status_counts": dict(statuses),
            "total_delivery_cost": round(total_cost, 2),
        }

    @task.branch
    def choose_processing_path(summary):
        threshold = int(get_current_context()["params"]["large_batch_threshold"])
        if summary["row_count"] >= threshold:
            return "process_large_batch"
        return "process_small_batch"

    @task
    def process_small_batch(summary):
        return _write_report("small", summary)

    @task
    def process_large_batch(summary):
        return _write_report("large", summary)

    @task(trigger_rule="none_failed_min_one_success")
    def processing_completed():
        return "Selected processing branch completed"

    @task(
        retries=2,
        retry_delay=timedelta(seconds=10),
        on_retry_callback=notify_retry,
    )
    def retry_demo():
        context = get_current_context()
        if context["task_instance"].try_number == 1:
            raise AirflowException("Temporary test error; retry should recover")
        return "Recovered after retry"

    def _write_report(batch_type, summary):
        context = get_current_context()
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        output_file = OUTPUT_DIR / f"{_safe_run_id(context)}-{batch_type}.json"
        report = {
            "batch_type": batch_type,
            "created_at": datetime.now(timezone.utc).isoformat(),
            **summary,
        }
        output_file.write_text(
            json.dumps(report, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
        return str(output_file)

    summary = read_and_analyze_csv()
    route = choose_processing_path(summary)
    small_batch = process_small_batch(summary)
    large_batch = process_large_batch(summary)
    route >> [small_batch, large_batch]

    completed = processing_completed()
    [small_batch, large_batch] >> completed
    completed >> retry_demo()


customer_delivery_batch()
