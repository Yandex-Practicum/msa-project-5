import csv
from datetime import datetime, timedelta
from pathlib import Path

from airflow import DAG
from airflow.operators.email import EmailOperator
from airflow.operators.python import BranchPythonOperator, PythonOperator
from airflow.utils.trigger_rule import TriggerRule


def choose_branch():
    source = Path("/opt/airflow/data/orders.csv")
    with source.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    total = sum(float(row["amount"]) for row in rows)
    return "accepted" if total >= 100 else "rejected"


def accepted():
    print("Порог суммы заказов достигнут")


def rejected():
    raise ValueError("Порог суммы заказов не достигнут")


with DAG(
    dag_id="marketing_poc",
    start_date=datetime(2026, 1, 1),
    schedule=None,
    catchup=False,
    default_args={"retries": 2, "retry_delay": timedelta(seconds=10)},
) as dag:
    route = BranchPythonOperator(task_id="read_and_branch", python_callable=choose_branch)
    good = PythonOperator(task_id="accepted", python_callable=accepted)
    bad = PythonOperator(task_id="rejected", python_callable=rejected)
    success = EmailOperator(
        task_id="success_email",
        to="marketing@example.local",
        subject="Пайплайн выполнен",
        html_content="Данные обработаны успешно.",
        trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS,
    )
    failure = EmailOperator(
        task_id="failure_email",
        to="marketing@example.local",
        subject="Ошибка пайплайна",
        html_content="Обработка завершилась ошибкой.",
        trigger_rule=TriggerRule.ONE_FAILED,
    )
    route >> [good, bad]
    good >> [success, failure]
    bad >> [success, failure]
