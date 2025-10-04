from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.empty import EmptyOperator
from airflow.operators.email import EmailOperator
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.providers.apache.kafka.operators.produce import ProduceToTopicOperator
from airflow.providers.apache.kafka.operators.consume import ConsumeFromTopicOperator
from airflow.utils.trigger_rule import TriggerRule
import pandas as pd


default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'email': ['admin@example.com'],
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=1),
}

def read_csv_func(ti):
    try:
        #raise Exception("TEST: CSV read failed")
        df = pd.read_csv('/opt/airflow/data/example.csv')
        ti.xcom_push(key='csv_data', value=df.to_dict('records'))
        return "CSV read successfully"
    except Exception as e:
        error_msg = f"Failed to read CSV: {str(e)}"
        ti.xcom_push(key='pipeline_error', value=error_msg)
        raise Exception(error_msg)

def read_pg_func(ti):
    try:
        hook = PostgresHook(postgres_conn_id='data_postgres')
        df = hook.get_pandas_df(sql='SELECT * FROM orders;')
        ti.xcom_push(key='pg_data', value=df.to_dict('records'))
        return "PostgreSQL read successfully"
    except Exception as e:
        error_msg = f"Failed to read from PostgreSQL: {str(e)}"
        ti.xcom_push(key='pipeline_error', value=error_msg)
        raise Exception(error_msg)

def producer_function():
    return [
        ('order_events', b'order modified for user 1'),
        ('order_events', b'order modified for user 2')
    ]

def consumer_function(message, **kwargs):
    try:
        if not isinstance(message, list):
            message = [message]

        decoded_messages = []
        for msg in message:
            value = msg.value()
            if isinstance(value, bytes):
                value = value.decode('utf-8')
            decoded_messages.append(value)

        return decoded_messages
    except Exception as e:
        error_msg = f"Failed to consume from Kafka: {str(e)}"
        return {"error": error_msg}

def collect_results_func(ti):
    try:
        csv_data = ti.xcom_pull(key='csv_data', task_ids='read_csv')
        pg_data = ti.xcom_pull(key='pg_data', task_ids='read_pg')
        kafka_data = ti.xcom_pull(task_ids='consume_kafka')
        
        if isinstance(kafka_data, dict) and 'error' in kafka_data:
            raise Exception(kafka_data['error'])
        
        ti.xcom_push(key='all_data', value={
            'csv_records': len(csv_data) if csv_data else 0,
            'pg_records': len(pg_data) if pg_data else 0,
            'kafka_messages': kafka_data if kafka_data else []
        })
        return "Results collected successfully"
    except Exception as e:
        error_msg = f"Failed to collect results: {str(e)}"
        ti.xcom_push(key='pipeline_error', value=error_msg)
        raise Exception(error_msg)

with DAG(
    dag_id='poc_pipeline',
    default_args=default_args,
    description='Simple PoC for data processing pipeline',
    schedule_interval=None,
    start_date=datetime(2025, 10, 1),
    catchup=False,
) as dag:

    start = EmptyOperator(task_id='start')

    read_csv = PythonOperator(
        task_id='read_csv',
        python_callable=read_csv_func
    )

    read_pg = PythonOperator(
        task_id='read_pg',
        python_callable=read_pg_func
    )

    produce_kafka = ProduceToTopicOperator(
        task_id='produce_kafka',
        kafka_config_id='kafka_default',
        topic='order_events',
        producer_function=producer_function,
        synchronous=False,
    )

    consume_kafka = ConsumeFromTopicOperator(
        task_id='consume_kafka',
        topics=['order_events'],
        apply_function=consumer_function,
        kafka_config_id='kafka_default',
        commit_cadence='end_of_batch',
        max_messages=10,
        max_batch_size=10,
    )

    collect_results = PythonOperator(
        task_id='collect_results',
        python_callable=collect_results_func
    )

    email_success = EmailOperator(
        task_id='email_success',
        to='admin@example.com',
        subject='Pipeline Completed Successfully',
        html_content="""
        <h3>Data Processing Pipeline Completed Successfully</h3>
        <p>All tasks executed without errors.</p>
        <p>Data Summary:</p>
        <ul>
            <li>CSV records processed: {{ ti.xcom_pull(key='all_data').csv_records }}</li>
            <li>Database records processed: {{ ti.xcom_pull(key='all_data').pg_records }}</li>
            <li>Kafka messages processed: {{ ti.xcom_pull(key='all_data').kafka_messages|length }}</li>
        </ul>
        """,
        trigger_rule=TriggerRule.ALL_SUCCESS
    )

    email_failure = EmailOperator(
        task_id='email_failure',
        to='admin@example.com',
        subject='PIPELINE FAILED',
        html_content="""
        <h3 style="color: red;">Data Processing Pipeline Failed</h3>
        <p><strong>Error Details:</strong></p>
        <p>{{ ti.xcom_pull(key='pipeline_error', task_ids=['read_csv', 'read_pg', 'collect_results']) | first | default('Unknown error occurred') }}</p>
        <p>Please check the Airflow logs for more details.</p>
        """,
        trigger_rule=TriggerRule.ONE_FAILED
    )

    start >> [read_csv, read_pg, produce_kafka]
    produce_kafka >> consume_kafka
    [read_csv, read_pg, consume_kafka] >> collect_results >> [email_success, email_failure]