# Task 1

## Problem description
A marketing department needs a flexible solution for their data processing pipeline, which should:
1. integrate with third-party APIs:
  * BigQuery;
  * Redshift;
  * Kafka;
  * Spark

2. have:
  * monitoring;
  * notifications system.

3. handle 1m rows per a single launch;
4. support cloud deployment.

## Proposed solution

[Apache Airflow](https://airflow.apache.org/) looks a prominent candidate.
The table below explains why.

| Requirement | Is met? | Comment |
| -- | -- | -- |
| Third-party APIs integration | ✅ | `Apache Airflow` has connectors for [BigQuery](https://airflow.apache.org/docs/apache-airflow-providers-google/stable/index.html), [Amazon Redshift](https://airflow.apache.org/docs/apache-airflow-providers-amazon/), [Kafka](https://airflow.apache.org/docs/apache-airflow-providers-apache-kafka/stable/index.html), [Spark](https://airflow.apache.org/docs/apache-airflow-providers-apache-spark/stable/index.html) |
| Branching, conditional operators and event triggers | ✅ | Branching is supported through [BranchPythonOperator](https://airflow.apache.org/docs/apache-airflow-providers-standard/stable/_api/airflow/providers/standard/operators/python/index.html#airflow.providers.standard.operators.python.BranchPythonOperator), conditions could be implemented through [ShortCircuitOperator](https://airflow.apache.org/docs/apache-airflow-providers-standard/stable/_api/airflow/providers/standard/operators/python/index.html#airflow.providers.standard.operators.python.ShortCircuitOperator) using a `callable`, trigger events are expressed through [Sensors](https://airflow.apache.org/docs/apache-airflow/stable/core-concepts/sensors.html) and [Deferrable](https://airflow.apache.org/docs/apache-airflow/stable/authoring-and-scheduling/deferring.html) operators |
| Fallback-logic, retries and notifications | ✅ | Fallback is supported through [on_failure_callback](https://airflow.apache.org/docs/apache-airflow/stable/administration-and-deployment/logging-monitoring/callbacks.html#callback-types) available at a task level, `retry` is supported as a task argument, email notification is supported through [EmailOperator](https://airflow.apache.org/docs/apache-airflow/1.10.6/_api/airflow/operators/email_operator/index.html) |
| Cloud deployment | ✅ | `Apache Airflow` could be used either as a managed solution, for example, [Yandex Managed Service for Apache Airflow](https://yandex.cloud/ru/services/managed-airflow), and [Google Cloud Composer](https://cloud.google.com/composer) or as a [self-hosted](https://airflow.apache.org/docs/apache-airflow/stable/administration-and-deployment/kubernetes.html) solution in a `k8s` cluster |

## POC
A video with a proof of concept was recorded and saved in a folder at Yandex.Disk, because it's a big (`~140MB`) `.mov` file.
The video is accessible at https://disk.yandex.ru/i/NYrMNCERJBi71Q