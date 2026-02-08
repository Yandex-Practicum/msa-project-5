# Обоснование выбора решения для пакетной обработки данных

## Выбранное решение: Apache Airflow

Apache Airflow выбран как инструмент оркестрации пакетных пайплайнов по следующим причинам:

### Основные преимущества

- Поддержка DAG (Directed Acyclic Graph)
- Возможность гибкого ветвления
- Поддержка retry и fallback
- Встроенные механизмы алертинга
- Масштабирование в Kubernetes

### Интеграции

Airflow имеет готовые инструменты для работы с различными источниками данных:
- BigQuery (BigQueryOperator)
- Amazon Redshift (PostgresOperator)
- Apache Kafka (KafkaConsumerOperator)
- Apache Spark (SparkSubmitOperator)

### Надёжность

- Retry-политики
- SLA monitoring
- Email-уведомления
- Failure callbacks

### Развёртывание в облаке

Решение разворачивается в Kubernetes с использованием Helm-чарта.
Поддерживает Celery/Kubernetes Executor.
Интегрируется с Prometheus и Grafana.

### Вывод

Apache Airflow полностью удовлетворяет требованиям проекта и подходит для обработки до 1 млн записей за запуск.
