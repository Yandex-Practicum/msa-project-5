FROM apache/airflow:2.7.0
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && apt-get autoremove -yqq --purge \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
USER airflow