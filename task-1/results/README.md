# Почему Apache Airflow

## Контекст задачи
Маркетинговый отдел компании расширяет обработку клиентских данных, объединяя информацию из разных источников:
- **CSV**-файлы со статусами доставок (файловое хранилище)
- **PostgreSQL** с информацией о заказах и платежах
- **Kafka** с событиями изменения заказов
- Внешние API и аналитические DWH (**BigQuery**, **Redshift**)

Требования к системе обработки:
- Гибкая оркестрация и построение пайплайнов
- Поддержка интеграций с BigQuery, Redshift, Kafka, Spark
- Встроенный мониторинг и оповещения
- Возможность ветвлений, условных операторов и event-triggers
- Поддержка retry, fallback-logic и уведомлений «из коробки»
- Масштабируемое и надёжное облачное развёртывание

Ожидаемый объём данных на один запуск — **~1 млн записей**.

---

## Почему именно Airflow

### 1. Богатая экосистема интеграций
- **BigQuery**: готовые операторы (`BigQueryInsertJobOperator`, `GCSToBigQueryOperator`).
- **Redshift**: (`RedshiftSQLOperator`, `S3ToRedshiftOperator`).
- **Kafka**: сенсоры и операторы (`KafkaConsumerRecordSensor`, `ProduceToTopicOperator`).
- **Spark**: (`SparkSubmitOperator`, `DatabricksSubmitRunOperator`, `DataprocSubmitJobOperator`).
- **Внешние API**: (`SimpleHttpOperator`, `PythonOperator`, `KubernetesPodOperator`).

Это позволяет объединять источники **GCP**, **AWS** и **on-premise** в одном пайплайне.

---

### 2. Поддержка сложной логики
- **Ветвления**: `BranchPythonOperator`, `ShortCircuitOperator`.
- **Условные переходы** и динамические DAG-и.
- **Event-triggers**: сенсоры (Kafka, S3/GCS), Dataset-события (Airflow 2.4+).

---

### 3. Надёжность и отказоустойчивость
- **Retry** и backoff-стратегии на уровне задач.
- **Fallback-ветви** через `trigger_rule=one_failed`.
- SLA и автоматическая эскалация при нарушении дедлайнов.

---

### 4. Мониторинг и оповещения
- Веб-интерфейс для наблюдения за DAG-ами и задачами.
- Email/Slack/Teams-уведомления через готовые операторы.
- Экспорт метрик в **Prometheus/Grafana**.

---

### 5. Облачное развёртывание
- **KubernetesExecutor** и официальные Helm-чарты.
- Поддержка секретов (AWS Secrets Manager, GCP Secret Manager).
- Возможность выбора managed-решений:
    - **Cloud Composer (GCP)**
    - **MWAA (AWS)**

Airflow нейтрален к облаку, что делает его оптимальным для **multi-cloud архитектуры**.

---

## Альтернативы и почему не они
- **AWS Glue / GCP Dataflow** — хороши внутри одного облака, но сложны для multi-cloud сценариев.
- **Dagster / Prefect** — современнее в DX, но менее зрелая экосистема операторов и меньше production-кейсов.
- **Databricks Workflows** — идеально для Spark/Delta Lake, но не так удобно для оркестрации BigQuery + Redshift + Kafka одновременно.

---

## Вывод
**Apache Airflow** — это зрелый, гибкий и расширяемый инструмент оркестрации, который:
- Обеспечивает богатый набор готовых интеграций (BigQuery, Redshift, Kafka, Spark, API).
- Поддерживает сложную логику пайплайнов, ретраи, fallback-и и мониторинг «из коробки».
- Легко разворачивается в облаке и работает в multi-cloud архитектуре.

Таким образом, Airflow — оптимальный выбор для построения надежной и масштабируемой системы пакетной обработки данных.  
