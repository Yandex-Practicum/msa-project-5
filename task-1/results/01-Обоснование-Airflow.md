# Задание 1. Обоснование: Apache Airflow

## Почему Airflow

- Пайплайн описывается **кодом** (Python), граф зависимостей — **DAG**.
- Есть **ветвления** (`BranchPythonOperator`), **сенсоры** для event-driven сценариев (в т.ч. Kafka — через провайдеры), **retries** на задачах.
- Готовые **провайдеры** для облачных и аналитических систем ускоряют интеграцию.

## Интеграции (готовые модули)

| Система | Как подключается в Airflow |
|--------|-----------------------------|
| **BigQuery** | Пакет `apache-airflow-providers-google`, операторы `BigQuery*`, интеграция с GCP |
| **Redshift** | Пакет `apache-airflow-providers-amazon`, операторы для AWS/Redshift |
| **Kafka** | Пакет `apache-airflow-providers-apache-kafka`, сенсоры/операторы по топикам |
| **Spark** | `SparkSubmitOperator` (провайдер Apache Spark) для batch-задач |

## Ветвление, условия, «event-triggers»

- **Ветвление и условия** — `BranchPythonOperator`, условные рёбра графа.
- **События** — сенсоры (`FileSensor`, `KafkaSensor` и др.); для низкой задержки Airflow не замена полноценному stream-движку.

## Retry, fallback, email

- **Retry** — параметры `retries`, `retry_delay` в `default_args` или у задачи.
- **Email** — встроенный SMTP (`EmailOperator`, уведомления); для прод — интеграция с корпоративным SMTP или облачной почтой.
- **Fallback** — реализуется явными ветками DAG и `trigger_rule` (как в теории про правила запуска задач).

## Развёртывание в облаке

- **GCP**: Cloud Composer (управляемый Airflow на GKE).
- **AWS**: MWAA или свой Airflow на EKS/EC2.
- **Kubernetes**: `KubernetesExecutor` / официальные Helm-чарты.

## POC

Код и `docker-compose` — каталог [`../airflow`](../airflow). Шаги: чтение CSV, чтение PostgreSQL, ветвление, уведомления через **MailHog** (локальный SMTP-ловушка), retry на шагах.


