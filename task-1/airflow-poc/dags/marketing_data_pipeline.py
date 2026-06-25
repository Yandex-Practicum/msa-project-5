from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import BranchPythonOperator, PythonOperator
from airflow.operators.dummy import DummyOperator
from airflow.operators.email import EmailOperator
from airflow.utils.trigger_rule import TriggerRule
import pandas as pd
import os
import json

# Email конфигурация
SUCCESS_EMAIL = "team-success@example.com"
FAILURE_EMAIL = "team-failure@example.com"

def read_csv_data(**context):
    """Чтение данных из CSV файла"""
    csv_path = '/opt/airflow/data/deliveries.csv'

    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"CSV file not found: {csv_path}")

    df = pd.read_csv(csv_path)
    print(f"Successfully loaded {len(df)} records")
    print(f"Columns: {list(df.columns)}")

    # Сохраняем данные в XCom для использования в следующих задачах
    context['task_instance'].xcom_push(key='delivery_data',
                                       value=df.to_dict())
    context['task_instance'].xcom_push(key='record_count',
                                       value=len(df))

    return f"Loaded {len(df)} records"

def check_order_status(**context):
    """Проверка статуса заказов и определение ветвления"""
    df_dict = context['task_instance'].xcom_pull(key='delivery_data',
                                                  task_ids='read_csv')
    df = pd.DataFrame.from_dict(df_dict)

    # Анализируем данные
    delayed_count = len(df[df['status'] == 'delayed'])
    completed_count = len(df[df['status'] == 'completed'])
    failed_count = len(df[df['status'] == 'failed'])

    print(f"Orders analysis:")
    print(f"- Completed: {completed_count}")
    print(f"- Delayed: {delayed_count}")
    print(f"- Failed: {failed_count}")

    # Сохраняем результаты анализа
    analysis_results = {
        'total_orders': len(df),
        'delayed_count': delayed_count,
        'completed_count': completed_count,
        'failed_count': failed_count,
        'has_problems': delayed_count > 0 or failed_count > 0
    }

    context['task_instance'].xcom_push(key='analysis', value=analysis_results)

    # Определяем ветвление: если есть проблемы - идем по пути обработки проблем
    if analysis_results['has_problems']:
        return 'process_problematic_orders'
    else:
        return 'generate_clean_report'

def process_problematic_orders(**context):
    """Обработка проблемных заказов"""
    df_dict = context['task_instance'].xcom_pull(key='delivery_data',
                                                  task_ids='read_csv')
    df = pd.DataFrame.from_dict(df_dict)

    # Фильтруем проблемные заказы
    problematic = df[df['status'].isin(['delayed', 'failed'])]

    # Создаем отчет по проблемным заказам
    report_path = '/opt/airflow/data/problematic_orders_report.csv'
    problematic.to_csv(report_path, index=False)

    print(f"Problematic orders processed: {len(problematic)}")
    print(f"Report saved to: {report_path}")

    context['task_instance'].xcom_push(key='problematic_count',
                                       value=len(problematic))

    return f"Processed {len(problematic)} problematic orders"

def generate_clean_report(**context):
    """Генерация чистого отчета (все заказы в порядке)"""
    df_dict = context['task_instance'].xcom_pull(key='delivery_data',
                                                  task_ids='read_csv')
    df = pd.DataFrame.from_dict(df_dict)

    report_path = '/opt/airflow/data/clean_report.csv'
    df.to_csv(report_path, index=False)

    print(f"Clean report generated with {len(df)} records")
    return f"Clean report generated"

def prepare_summary(**context):
    """Подготовка итогового отчета"""
    analysis = context['task_instance'].xcom_pull(key='analysis',
                                                   task_ids='check_order_status')

    summary = f"""
    Marketing Data Processing Summary:
    =================================
    Total Orders Processed: {analysis.get('total_orders', 0)}
    Completed Orders: {analysis.get('completed_count', 0)}
    Delayed Orders: {analysis.get('delayed_count', 0)}
    Failed Orders: {analysis.get('failed_count', 0)}
    Status: {'PROBLEMS DETECTED' if analysis.get('has_problems') else 'ALL CLEAR'}
    """

    print(summary)

    # Сохраняем summary в файл
    with open('/opt/airflow/data/processing_summary.txt', 'w') as f:
        f.write(summary)

    return summary

# Определение DAG
default_args = {
    'owner': 'marketing_team',
    'depends_on_past': False,
    'email': [SUCCESS_EMAIL, FAILURE_EMAIL],
    'email_on_failure': True,
    'email_on_retry': False,
    'email_on_success': True,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
    'retry_exponential_backoff': True,
    'max_retry_delay': timedelta(hours=1),
    'start_date': datetime(2024, 1, 1),
}

with DAG(
    'marketing_data_processing',
    default_args=default_args,
    description='Marketing data processing pipeline with branching and notifications',
    schedule_interval='@daily',
    catchup=False,
    tags=['marketing', 'data-processing'],
    params={
        'environment': 'production',
        'notification_level': 'important'
    }
) as dag:

    # Начало пайплайна
    start = DummyOperator(task_id='start')

    # Чтение данных из CSV
    read_csv = PythonOperator(
        task_id='read_csv',
        python_callable=read_csv_data,
        retries=2,
        retry_delay=timedelta(seconds=30),
    )

    # Анализ данных и ветвление
    check_status = BranchPythonOperator(
        task_id='check_order_status',
        python_callable=check_order_status,
        retries=1,
        retry_delay=timedelta(minutes=1),
    )

    # Обработка проблемных заказов
    process_problematic = PythonOperator(
        task_id='process_problematic_orders',
        python_callable=process_problematic_orders,
        retries=3,
        retry_delay=timedelta(minutes=2),
    )

    # Генерация чистого отчета
    clean_report = PythonOperator(
        task_id='generate_clean_report',
        python_callable=generate_clean_report,
        retries=2,
        retry_delay=timedelta(minutes=1),
    )

    # Подготовка итогов
    prepare_summary = PythonOperator(
        task_id='prepare_summary',
        python_callable=prepare_summary,
        trigger_rule=TriggerRule.ONE_SUCCESS,  # Выполняется при успехе любой ветки
    )

    # Email уведомление об успехе
    success_email = EmailOperator(
        task_id='send_success_email',
        to=SUCCESS_EMAIL,
        subject='✅ Marketing Data Pipeline Success',
        html_content="""
        <h2>Marketing Data Processing Completed Successfully</h2>
        <p>The data processing pipeline has completed successfully.</p>
        <p>Check the summary report for details.</p>
        <br>
        <p><strong>Pipeline:</strong> marketing_data_processing</p>
        <p><strong>Time:</strong> {{ execution_date }}</p>
        """,
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # Email уведомление об ошибке
    failure_email = EmailOperator(
        task_id='send_failure_email',
        to=FAILURE_EMAIL,
        subject='❌ Marketing Data Pipeline Failed',
        html_content="""
        <h2>Marketing Data Processing Failed</h2>
        <p>The data processing pipeline has encountered an error.</p>
        <p>Please check the Airflow UI for more details.</p>
        <br>
        <p><strong>Pipeline:</strong> marketing_data_processing</p>
        <p><strong>Time:</strong> {{ execution_date }}</p>
        <p><strong>Error:</strong> Check logs for more information</p>
        """,
        trigger_rule=TriggerRule.ONE_FAILED,
    )

    # Конец пайплайна
    end = DummyOperator(
        task_id='end',
        trigger_rule=TriggerRule.ONE_SUCCESS
    )

    # Определение последовательности задач
    start >> read_csv >> check_status
    check_status >> [process_problematic, clean_report]
    [process_problematic, clean_report] >> prepare_summary >> [success_email, failure_email] >> end