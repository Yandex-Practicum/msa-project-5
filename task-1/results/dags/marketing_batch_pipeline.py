from __future__ import annotations

import csv
import random
from datetime import datetime, timedelta

from airflow import DAG
from airflow.exceptions import AirflowFailException
from airflow.providers.standard.operators.empty import EmptyOperator
from airflow.sdk import task, TriggerRule

DEFAULT_ARGS = {
    "owner": "marketing",
    "retries": 2,
    "retry_delay": timedelta(minutes=1),
}

with DAG(
        dag_id="marketing_batch_pipeline",
        start_date=datetime(2024, 1, 1),
        schedule=None,
        catchup=False,
        default_args=DEFAULT_ARGS,
        tags=["task1", "marketing", "batch"],
) as dag:
    start = EmptyOperator(task_id="start")
    end = EmptyOperator(task_id="end")


    @task
    def read_delivery_statuses() -> int:
        """Reads CSV file with delivery statuses"""
        with open("/opt/airflow/data/delivery_statuses.csv") as f:
            reader = csv.DictReader(f)
            return sum(1 for _ in reader)


    @task.branch
    def choose_processing_path(records_count: int) -> str:
        """Branch pipeline depending on records count"""
        if records_count > 5:
            return "heavy_processing"
        return "light_processing"


    @task
    def heavy_processing():
        print("Running heavy processing")
        if random.choice([False, False, True]):
            raise AirflowFailException("Simulated heavy processing failure")


    @task
    def light_processing():
        print("Running light processing")


    @task(trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS)
    def success_email():
        print("Pipeline completed successfully!")


    @task(trigger_rule=TriggerRule.ONE_FAILED)
    def failure_email():
        print("Pipeline failed!")


    records = read_delivery_statuses()
    branch = choose_processing_path(records)

    heavy = heavy_processing()
    light = light_processing()
    success_email_task = success_email()
    failure_email_task = failure_email()

    start >> records >> branch
    branch >> heavy >> success_email_task
    branch >> light >> success_email_task

    [heavy, light] >> failure_email_task
    success_email_task >> end
