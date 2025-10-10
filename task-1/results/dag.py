from airflow import DAG
from airflow.providers.smtp.operators.smtp import EmailOperator
from airflow.providers.standard.operators.empty import EmptyOperator
from airflow.providers.standard.operators.python import PythonOperator
from airflow.utils.timezone import datetime
from airflow.utils.trigger_rule import TriggerRule
import pandas as pd
from airflow.providers.postgres.hooks.postgres import PostgresHook
import logging

def csv(ti):
    try:
        csv_data = pd.read_csv('/opt/airflow/test.csv').to_dict('records')
        logging.info(f"csv_data: {csv_data}")
        ti.xcom_push(key='csv_data', value=pd.read_csv('/opt/airflow/test.csv').to_dict('records'))
        return "CSV read successfully"
    except Exception as e:
        ti.xcom_push(key='pipeline_error', value=str(e))
        raise e

def sql(ti):
    try:
        hook = PostgresHook(postgres_conn_id='postgres_connection')
        df = hook.get_pandas_df(sql='SELECT * FROM test;')
        ti.xcom_push(key='pg_data', value=df.to_dict('records'))
    except Exception as e:
        logging.info("QWEQWE")
        error_msg = f"Failed to read from PostgreSQL: {str(e)}"
        ti.xcom_push(key='pipeline_error', value=error_msg)
        raise Exception(error_msg)

with DAG(
        dag_id="dag_msa_5",
        start_date=datetime(2025, 1, 1),
        schedule="@daily",
        catchup=False,
) as dag:
    start = EmptyOperator(task_id='start')

    csv_data = PythonOperator(
        task_id='csv',
        python_callable=csv
    )

    sql_data = PythonOperator(
        task_id='sql',
        python_callable=sql,
    )

    end_operator = EmptyOperator(task_id='end', trigger_rule=TriggerRule.ALWAYS)

    email_success = EmailOperator(
        task_id='email_success',
        to='qwe@qwe.com',
        subject='Pipeline Completed Successfully',
        html_content="""
            csv: {{ ti.xcom_pull(key='csv_data') }}
            sql: {{ ti.xcom_pull(key='sql_data') }}
        """,
        trigger_rule=TriggerRule.ALL_SUCCESS
    )

    email_fail = EmailOperator(
        task_id='email_fail',
        to='qwe@qwe.com',
        subject='Pipeline fail',
        html_content="""
           {{ ti.xcom_pull(key='pipeline_error') }}
        """,
        trigger_rule=TriggerRule.ONE_FAILED
    )

    start >> [csv_data, sql_data] >> end_operator >> [email_success, email_fail]
