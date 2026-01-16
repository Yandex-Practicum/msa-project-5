FROM apache/airflow:2.8.4

ARG AIRFLOW_VERSION=2.8.4
ARG PYTHON_VERSION=3.8
ARG CONSTRAINT_URL="https://raw.githubusercontent.com/apache/airflow/constraints-${AIRFLOW_VERSION}/constraints-${PYTHON_VERSION}.txt"

COPY requirements.txt /requirements.txt

RUN pip install --no-cache-dir --constraint "${CONSTRAINT_URL}" -r /requirements.txt
