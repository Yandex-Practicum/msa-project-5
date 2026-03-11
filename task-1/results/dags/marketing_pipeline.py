from airflow import DAG
from airflow.operators.python import PythonOperator, BranchPythonOperator
from airflow.operators.email import EmailOperator
from airflow.utils.dates import days_ago
from airflow.exceptions import AirflowFailException
import os
import time

# --- Конфигурация ---
DEFAULT_ARGS = {
    'owner': 'marketing_dept',
    'depends_on_past': False,
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': 60,
}

# --- Логика задач ---

def read_source_data(**context):
    """
    Имитация чтения из CSV/PostgreSQL.
    В реальном проекте здесь был бы Hook (PostgresHook / S3Hook).
    """
    print("Чтение данных из источника (CSV/Postgres)...")
    # Возвращаем фиктивные данные для демонстрации
    data_status = "OK" 
    context['ti'].xcom_push(key='data_status', value=data_status)
    return data_status

def check_branch_condition(**context):
    """
    Логика ветвления.
    Если статус 'ERROR', идем на алерт, иначе на успешную обработку.
    """
    ti = context['ti']
    # Для демонстрации retry можно иногда возвращать ERROR, 
    # но для стабильности демо вернем OK, а retry покажем на другой задаче.
    status = ti.xcom_pull(key='data_status', task_ids='read_source')
    
    print(f"Анализ данных. Статус: {status}")
    
    if status == "ERROR":
        return 'send_alert_email'
    else:
        return 'process_data_success'

def process_data_with_retry(**context):
    """
    Задача с политикой Retry.
    Намеренно падает при первой попытке, чтобы продемонстрировать механизм.
    """
    attempt = context['task_instance'].try_number
    print(f"Попытка выполнения: {attempt}")
    
    if attempt == 1:
        print("Симуляция сбоя сети/БД. Вызываем ошибку для триггера Retry...")
        # Используем обычное Exception, чтобы сработал retry!
        raise Exception("Симулированный сбой для демонстрации Retry")
    
    print("Обработка данных успешна (после retry).")
    return "Success"

def success_callback(context):
    """
    Callback при успехе DAG.
    В реальности здесь отправка Email/Slack.
    """
    print("="*50)
    print(f"SUCCESS ALERT: Пайплайн {context['dag'].dag_id} завершен успешно!")
    print(f"Время: {context['execution_date']}")
    print("="*50)

def failure_callback(context):
    """
    Callback при неудаче DAG.
    """
    print("="*50)
    print(f"FAILURE ALERT: Пайплайн {context['dag'].dag_id} упал!")
    print(f"Причина: {context.get('exception')}")
    print("="*50)

# --- Определение DAG ---

with DAG(
    'marketing_pipeline_poc',
    default_args=DEFAULT_ARGS,
    description='POC для обработки данных маркетинга',
    schedule_interval=None,  # Запуск вручную для демо
    start_date=days_ago(1),
    catchup=False,
    on_success_callback=success_callback,
    on_failure_callback=failure_callback,
    tags=['marketing', 'poc'],
) as dag:

    # 1. Чтение данных
    read_task = PythonOperator(
        task_id='read_source',
        python_callable=read_source_data,
    )

    # 2. Ветвление
    branch_task = BranchPythonOperator(
        task_id='check_condition',
        python_callable=check_branch_condition,
    )

    # 3. Ветка Успеха (с Retry логикой внутри задачи для демо)
    process_success = PythonOperator(
        task_id='process_data_success',
        python_callable=process_data_with_retry,
        retries=2,  # Политика Retry
        retry_delay=5, # Быстрый retry для демо
    )

    # 4. Ветка Алерта
    alert_email = EmailOperator(
        task_id='send_alert_email',
        to='admin@company.com',
        subject='Alert: Ошибка в данных доставки',
        html_content='Обнаружены критические ошибки в статусах доставки.',
    )

    # 5. Финальный шаг (Join)
    # В Airflow ветки не объединяются автоматически, 
    # но для демо мы можем оставить их параллельными или добавить DummyOperator
    finish_task = PythonOperator(
        task_id='pipeline_finished',
        python_callable=lambda: print("Пайплайн завершен"),
        trigger_rule='none_failed_min_one_success' # Выполнится, если хотя бы одна ветка успешна
    )

    # Настройка зависимостей
    read_task >> branch_task
    branch_task >> [process_success, alert_email]
    process_success >> finish_task
    alert_email >> finish_task