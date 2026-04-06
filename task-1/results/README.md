# POC пакетной обработки на Airflow

## Почему выбран Apache Airflow

Для этого задания Apache Airflow является самым практичным выбором, потому что он решает задачу оркестрации, а не тяжелой обработки данных внутри себя. Ожидаемый объем около 1 млн записей за запуск для Airflow нормален, если фактические трансформации выполняются в SQL, Spark или внешних сервисах, а сам Airflow управляет шагами пайплайна, зависимостями, повторами и уведомлениями.

Почему Airflow подходит под требования:

- BigQuery: есть готовые провайдеры, операторы, сенсоры и хуки, включая `BigQueryInsertJobOperator` и `BigQueryCheckOperator`.
- Redshift: есть Amazon provider с SQL-операторами, хуками и типовыми интеграциями через S3.
- Kafka: есть Apache Kafka provider для сценариев чтения, публикации и event-driven оркестрации.
- Spark: есть `SparkSubmitOperator` и стандартный паттерн запуска Spark-задач из Airflow.
- Ветвление и условия: `BranchPythonOperator`, `ShortCircuitOperator`, trigger rules и Task Group.
- Event triggers: доступны sensors, datasets, REST API triggers, Kafka-ориентированные сценарии и меж-DAG зависимости.
- Retry и fallback: из коробки поддерживаются `retries`, `retry_delay`, failure callbacks и разветвление по trigger rules.
- Уведомления: есть встроенная SMTP-интеграция для email, а также типовые интеграции со Slack, Teams, PagerDuty и webhook.
- Мониторинг: web UI, логи, health checks, метрики и зрелые шаблоны эксплуатации в облаке.

Если сравнивать с Prefect или Dagster, Airflow здесь выигрывает прежде всего зрелостью экосистемы провайдеров и тем, что под требования enterprise-интеграций и облачного развертывания он подходит наиболее прямо.

## Что реализовано локально

В этой папке собран локальный POC, который демонстрирует:

- чтение данных из PostgreSQL;
- чтение данных из CSV-файла;
- объединение и анализ данных;
- ветвление пайплайна по условию;
- retry при временной ошибке;
- email-уведомление об успешном завершении;
- email-уведомление об ошибке через стандартные настройки Airflow.

## Структура проекта

- `docker-compose.yml`: локальный стенд с Airflow, PostgreSQL и MailHog.
- `airflow/Dockerfile`: образ Airflow с PostgreSQL provider.
- `airflow/dags/marketing_batch_poc.py`: демонстрационный DAG.
- `airflow/include/delivery_statuses.csv`: файловый источник со статусами доставок.
- `postgres/init/01_marketing_demo.sql`: схема и тестовые данные.
- `airflow/output/`: артефакты, которые генерирует DAG.
- `results/README.md`: исходный текст задания.

## Архитектура

![[readme.png]]
## Интеграции и развертывание в облаке

### BigQuery

Для BigQuery используются провайдеры Google и операторы наподобие `BigQueryInsertJobOperator`. В облаке Airflow-аутентификация обычно строится через service account или workload identity. Загрузки могут идти напрямую в BigQuery или через GCS.

### Redshift

Для Redshift используется Amazon provider. На практике загрузка обычно строится через S3 staging плюс SQL-операторы или COPY-команды. В проде соединения и секреты должны храниться во внешнем секрет-хранилище.

### Kafka

Kafka может быть как источником событий, так и механизмом запуска пайплайна. Airflow может работать через sensors, внешние триггеры или сценарии, где запуск DAG происходит после закрытия batch-окна.

### Spark

Тяжелые трансформации следует выносить в Spark-задачи, а Airflow использовать как оркестратор. Для этого подходят `SparkSubmitOperator` или Kubernetes-ориентированные Spark-интеграции.