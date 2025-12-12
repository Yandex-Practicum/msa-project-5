import pandas as pd
import logging
import os
from datetime import datetime, timedelta

from airflow.decorators import dag, task
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.utils.email import send_email
from airflow.exceptions import AirflowException


DATA_DIR = '/opt/airflow/data'
DEFAULT_ARGS = {
    'owner': 'demo_user',
    'retries': 3,
    'retry_delay': timedelta(seconds=30)
}

def on_failure_callback(context):
    """Called automatically when the DAG fails permanently."""
    dag_run = context.get('dag_run')
    task_instance = context.get('task_instance')
    msg = f"Task {task_instance.task_id} failed. Pipeline: {dag_run.dag_id}"
    send_email(to='admin@demo.com', subject='Pipeline Alert', html_content=msg)

@dag(
    dag_id='ariflow_demo',
    schedule='@daily',
    catchup=False,
    default_args=DEFAULT_ARGS,
    on_failure_callback=on_failure_callback,
    tags=['demo', 'msa-sprint-5']
)
def customer_analysis_pipeline():
    @task(
        task_id='extract_transform',
        retries=5,                          
        retry_delay=timedelta(seconds=10)
    )
    def extract_and_filter() -> int:
        logging.info("Starting extraction from a db and CSV...")
        
        csv_path = os.path.join(DATA_DIR, 'deliveries.csv')
        if not os.path.exists(csv_path):
            raise AirflowException(f"CSV Missing: {csv_path}")
        
        df_csv = pd.read_csv(csv_path)
        logging.info(f"Loaded {len(df_csv)} rows from CSV.")

        try:
            pg_hook = PostgresHook(postgres_conn_id='demo_db')
            query = """
                select
                    c.client_id, c.name, c.email,
                    o.order_id, o.amount
                from clients c
                join orders o ON c.client_id = o.client_id
            """
            df_db = pg_hook.get_pandas_df(query)
            logging.info(f"Loaded {len(df_db)} rows from DB.")
        except Exception as e:
            raise AirflowException(f"Database connection failed: {e}")

        merged = pd.merge(df_db, df_csv, on='order_id', how='left')
        
        stats = merged.groupby(['client_id', 'name']).agg({
            'amount': 'mean'
        }).reset_index()

        # 💡 Keep only those records where an average paycheck > $100
        high_value = stats[stats['amount'] > 100]
        
        out_path = os.path.join(DATA_DIR, 'result_high_value.csv')
        high_value.to_csv(out_path, index=False)
        
        count = len(high_value)
        logging.info(f"Analysis complete. Found {count} high value clients.")
        
        return count

    @task.branch(task_id='check_results')
    def branch_logic(count: int) -> str:
        if count > 0:
            return 'notify_success'
        return 'notify_failure_no_data'

    @task(
        task_id='notify_success',
        retries=3, 
        retry_delay=timedelta(minutes=2)
    )
    def notify_success():
        send_email(
            to='manager@demo.com',
            subject='Analysis Success',
            html_content='<b>Success:</b> High value customers identified. Check output file.'
        )

    @task(task_id='notify_failure_no_data')
    def notify_no_data():
        send_email(
            to='manager@demo.com',
            subject='Analysis Empty',
            html_content='Pipeline ran successfully, but no customers met criteria.'
        )

    result_count = extract_and_filter()
    next_step = branch_logic(result_count)
    next_step >> [notify_success(), notify_no_data()]

customer_analysis_pipeline()
