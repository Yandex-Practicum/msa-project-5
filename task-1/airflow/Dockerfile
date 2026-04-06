ARG AIRFLOW_VERSION=2.10.5
ARG PYTHON_VERSION=3.11

FROM apache/airflow:${AIRFLOW_VERSION}-python${PYTHON_VERSION}

USER airflow

RUN pip install --no-cache-dir \
    --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-${AIRFLOW_VERSION}/constraints-3.11.txt" \
    apache-airflow-providers-postgres
