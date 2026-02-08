from airflow import DAG
from airflow.operators.python import PythonOperator, BranchPythonOperator
from airflow.operators.email import EmailOperator
from datetime import datetime
import random


def read_data():
    print("Reading data from source")


def analyze_data():
    result = random.choice(["success", "fail"])
    print("Analyze result:", result)

    if result == "success":
        return "process_success"
    return "process_fail"


def process_success():
    print("Processing success branch")


def process_fail():
    print("Processing fail branch")


default_args = {
    "owner": "airflow",
    "retries": 2,
    "email_on_failure": True,
    "email": ["test@example.com"]
}


with DAG(
        dag_id="batch_processing_pipeline",
        start_date=datetime(2024, 1, 1),
        schedule_interval=None,
        catchup=False,
        default_args=default_args,
        tags=["batch", "demo"]
):

    read = PythonOperator(
        task_id="read_data",
        python_callable=read_data
    )

    branch = BranchPythonOperator(
        task_id="analyze",
        python_callable=analyze_data
    )

    success = PythonOperator(
        task_id="process_success",
        python_callable=process_success
    )

    fail = PythonOperator(
        task_id="process_fail",
        python_callable=process_fail
    )

    notify = EmailOperator(
        task_id="notify",
        to="test@example.com",
        subject="Pipeline finished",
        html_content="Batch pipeline finished"
    )

    read >> branch >> [success, fail] >> notify
