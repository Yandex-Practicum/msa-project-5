# Task 1: Обоснование выбора технологического решения для пакетной обработки данных

## Выбранное решение: Apache Airflow

### Почему Apache Airflow

Apache Airflow - это платформа для оркестрации рабочих процессов (workflow orchestration), которая позволяет программно создавать, планировать и мониторить пайплайны обработки данных. Пайплайны описываются как графы на Python.

### Обоснование выбора

1. Интеграция с требуемыми системами

Airflow имеет готовые провайдеры (модули) для всех указанных систем:

- `apache-airflow-providers-google` - BigQuery, GCS, Cloud Composer
- `apache-airflow-providers-amazon` - Redshift, S3
- `apache-airflow-providers-apache-kafka` - Kafka consumer/producer
- `apache-airflow-providers-apache-spark` - Spark Submit, Spark SQL

Это значительно ускоряет разработку - не нужно писать интеграции с нуля.

2. Ветвление, условные операторы и event-triggers

- Ветвление: `BranchPythonOperator` позволяет выбирать путь выполнения на основе условий (например, если в отчете > 5000 строк - запускать пакетную обработку, иначе - построчную).
- Условные операторы: `ShortCircuitOperator` прерывает выполнение ветки при невыполнении условия. `trigger_rule` позволяет задавать правила запуска задач (all_success, one_failed, none_failed и т.д.).
- Event-triggers: Sensors ожидают наступления событий (появление файла в GCS, запись в БД). Dataset-triggered DAGs запускаются при обновлении данных другим DAG.

3. Retry, fallback и email-уведомления из коробки

```python
default_args = {
    'retries': 3,                          # Количество повторных попыток
    'retry_delay': timedelta(minutes=5),   # Задержка между попытками
    'retry_exponential_backoff': True,     # Экспоненциальный backoff
    'email': ['ops@tradeware.ru'],         # Email для уведомлений
    'email_on_failure': True,              # Уведомление при ошибке
    'email_on_retry': True,                # Уведомление при повторе
    'on_failure_callback': alert_slack,    # Кастомный callback
}
```

Fallback-логика реализуется через `trigger_rule='one_failed'` - задача запускается, когда предыдущая завершилась с ошибкой.

4. Развертывание в облачной среде

TradeWare уже использует GCP. Google Cloud Composer - это полностью управляемый сервис Apache Airflow на GCP:

- Автоматическое масштабирование воркеров под нагрузку
- Нативная интеграция с GCS (где уже хранятся файлы отчетов), BigQuery, Pub/Sub
- Встроенный мониторинг через Cloud Monitoring и Cloud Logging
- Не требует управления инфраструктурой - DevOps команда может сосредоточиться на миграции монолита

Альтернативный вариант - развертывание Airflow в GKE (Google Kubernetes Engine) с использованием Helm-чарта.

5. Соответствие стратегии миграции

Airflow оркестрирует процессы вокруг Java-приложения:

- Может вызывать Java-микросервисы через REST API (`SimpleHttpOperator`)
- Может запускать Java-задачи через `DockerOperator` или `KubernetesPodOperator`
- Централизованный мониторинг через UI
- По мере выноса функционала из монолита в микросервисы, Airflow будет оркестрировать их взаимодействие


## POC

### Запуск

```bash
# Инициализация Airflow (первый запуск)
docker compose up airflow-init

# Запуск всех сервисов
docker compose up -d

# Airflow UI: http://localhost:8080
# Логин: airflow / Пароль: airflow
```

### Что демонстрирует POC

1. DAG читает CSV-файл из локальной директории (аналог GCS) и данные из PostgreSQL.
2. `BranchPythonOperator` анализирует количество строк в файле и направляет обработку по одному из двух путей (пакетная или построчная обработка).
3. Настроены callback-и `on_success_callback` и `on_failure_callback` для отправки уведомлений. В локальном POC используется логирование (в продакшене подключается SMTP).
4. Каждый шаг пайплайна настроен с `retries=3` и `retry_delay=30s` с экспоненциальным backoff.

### Переход к продакшену

Для развертывания в GCP:

1. Создать окружение Cloud Composer в консоли
2. Загрузить DAG-файл в бакет GCS окружения
3. Настроить Connections в Airflow UI (PostgreSQL, GCS, SMTP)
4. Настроить переменные (пороги, email-адреса)
5. Cloud Composer автоматически масштабирует воркеры под нагрузку
