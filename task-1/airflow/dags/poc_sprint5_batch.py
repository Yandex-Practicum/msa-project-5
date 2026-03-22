"""
POC по заданию 1: чтение CSV + чтение PostgreSQL, ветвление, письма через MailHog, retry на шагах.
См. теорию: Apache Airflow — DAG, Workflow as Code, BranchPythonOperator, retries.
"""
from datetime import timedelta

from airflow import DAG
from airflow.operators.python import BranchPythonOperator, PythonOperator
from airflow.utils.dates import days_ago


def read_csv(**context):
    path = "/opt/airflow/data/sample.csv"
    with open(path, encoding="utf-8") as f:
        n = len(f.readlines())
    context["ti"].xcom_push(key="csv_rows", value=n)
    return n


def read_db(**context):
    import psycopg2

    conn = psycopg2.connect(
        host="postgres",
        port=5432,
        dbname="demo",
        user="demo",
        password="demo",
    )
    try:
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) FROM orders")
        count = cur.fetchone()[0]
    finally:
        conn.close()
    context["ti"].xcom_push(key="db_orders", value=int(count))
    return int(count)


def choose_branch(**context):
    n = int(context["ti"].xcom_pull(task_ids="read_csv", key="csv_rows"))
    return "send_ok_email" if n > 0 else "send_fail_email"


def _send_mail(subject: str, body: str) -> None:
    import smtplib
    from email.message import EmailMessage

    msg = EmailMessage()
    msg["Subject"] = subject
    msg["To"] = "reviewer@test.local"
    msg["From"] = "airflow@local.test"
    msg.set_content(body)
    with smtplib.SMTP("mailhog", 1025) as s:
        s.send_message(msg)


def email_ok():
    _send_mail("POC: успех", "Ветка: в CSV есть строки, пайплайн завершён успешно.")


def email_fail():
    _send_mail("POC: ветка без данных", "Ветка: CSV пустой (демонстрация уведомления).")


default_args = {
    "owner": "sprint5",
    "retries": 2,
    "retry_delay": timedelta(seconds=15),
}

with DAG(
    dag_id="poc_sprint5_batch",
    default_args=default_args,
    schedule=None,
    start_date=days_ago(1),
    catchup=False,
    tags=["sprint5", "poc"],
) as dag:
    read_csv_task = PythonOperator(
        task_id="read_csv",
        python_callable=read_csv,
    )
    read_db_task = PythonOperator(
        task_id="read_db",
        python_callable=read_db,
    )
    branch = BranchPythonOperator(
        task_id="branch",
        python_callable=choose_branch,
    )
    send_ok = PythonOperator(task_id="send_ok_email", python_callable=email_ok)
    send_fail = PythonOperator(task_id="send_fail_email", python_callable=email_fail)

    read_csv_task >> read_db_task >> branch >> [send_ok, send_fail]
