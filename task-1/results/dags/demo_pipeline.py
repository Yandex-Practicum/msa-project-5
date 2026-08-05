"""
POC-пайплайн пакетной обработки

Эмулирует ежедневную обработку файла со статусами доставок:
читает источник -> ветвится по условию -> при проблемах выполняет
шаг с retry-политикой -> уведомляет по email об успехе/неуспехе прогона.
"""
from datetime import datetime, timedelta

from airflow import DAG
from airflow.decorators import task
from airflow.models import Variable
from airflow.operators.email import EmailOperator
from airflow.utils.trigger_rule import TriggerRule

SOURCE_PATH = "/opt/airflow/data/source.csv"
NOTIFY_EMAIL = "team@demo.local"

default_args = {
    "owner": "data-eng",
    "retries": 0,
}


@task
def read_source():
    import csv

    with open(SOURCE_PATH, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    print(f"Прочитано строк: {len(rows)}")
    return rows


@task.branch
def check_condition(rows):
    failed = [r for r in rows if r.get("delivery_status") == "failed"]
    print(f"Строк со статусом failed: {len(failed)} из {len(rows)}")
    return "handle_failures" if failed else "process_normal"


@task
def process_normal():
    print("Все доставки в порядке, обычная обработка завершена.")


@task(task_id="handle_failures", retries=3, retry_delay=timedelta(seconds=10))
def flaky_retry_step(ti=None):
    # Variable "force_fail" (Admin -> Variables в Airflow UI) позволяет
    # на втором прогоне намеренно исчерпать все retry и увидеть email об ошибке,
    # не редактируя код DAG между демонстрациями.
    force_fail = Variable.get("force_fail", default_var="false").lower() == "true"
    try_number = ti.try_number
    print(f"Попытка №{try_number}, force_fail={force_fail}")
    if force_fail or try_number < 3:
        raise RuntimeError(f"Симуляция сбоя внешнего сервиса (попытка {try_number})")
    print("Внешний сервис отработал успешно, проблемные доставки обработаны.")


@task(trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS)
def join():
    print("Ветки сошлись, финальный шаг.")


with DAG(
    dag_id="batch_processing_demo",
    description="POC: источник -> ветвление -> retry -> email-уведомления",
    start_date=datetime(2026, 1, 1),
    schedule=None,
    catchup=False,
    default_args=default_args,
    tags=["task-1", "poc"],
) as dag:

    rows = read_source()
    branch = check_condition(rows)

    t_normal = process_normal()
    t_handle_failures = flaky_retry_step()
    t_join = join()

    t_notify_success = EmailOperator(
        task_id="notify_success",
        to=NOTIFY_EMAIL,
        subject="[Airflow] batch_processing_demo: успешно",
        html_content="Пайплайн обработки статусов доставок успешно завершён.",
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    t_notify_failure = EmailOperator(
        task_id="notify_failure",
        to=NOTIFY_EMAIL,
        subject="[Airflow] batch_processing_demo: ОШИБКА",
        html_content="Пайплайн обработки статусов доставок завершился с ошибкой после исчерпания retry.",
        trigger_rule=TriggerRule.ONE_FAILED,
    )

    branch >> [t_normal, t_handle_failures]
    [t_normal, t_handle_failures] >> t_join
    t_join >> t_notify_success
    [rows, branch, t_normal, t_handle_failures, t_join] >> t_notify_failure
