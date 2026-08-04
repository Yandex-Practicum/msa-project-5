# Task 1.1. Выбор решения для пакетной обработки

## Сравнение и выбор

| Критерий | Apache Airflow | Spring Batch | Spring Batch + Spring Cloud Data Flow |
| --- | --- | --- | --- |
| Тип | Оркестратор data pipelines | Java batch-фреймворк | Batch-фреймворк и платформа управления jobs |
| Описание процесса | Python DAG | Java Job/Step | Java Job/Step и управление через SCDF |
| Ветвление и события | Branching, trigger rules, sensors и event triggers | Условные переходы; внешний запуск | Условные переходы; API, расписание или события |
| Retry и fallback | Настройки tasks, callbacks и альтернативные ветки | Retry и переходы между steps | Средства Spring Batch |
| Email и мониторинг | SMTP callbacks, Web UI и логи | Spring Mail и метрики; UI отсутствует | SCDF UI, Micrometer и Spring Mail |
| Интеграции с data-системами | Готовые provider-пакеты | JDBC, Spring-библиотеки или адаптеры | Реализуются внутри batch-приложений |
| Локальный запуск | Docker Compose | Spring Boot или Docker Compose | Более тяжёлое окружение SCDF |
| Облако | Managed Airflow или Kubernetes | Контейнер или Kubernetes Job | Kubernetes |

Выбран **Apache Airflow — оркестратор пакетных пайплайнов**. Он лучше соответствует задаче с разнородными источниками, ветвлением, событиями, уведомлениями и наблюдением за выполнением. Spring Batch больше подходит для реализации обработки внутри Java-приложения, а Spring Cloud Data Flow — для управления такими приложениями.

## Требуемые интеграции

| Система | Тип | Готовый модуль Airflow | Интеграция |
| --- | --- | --- | --- |
| BigQuery | Serverless аналитическое хранилище | `apache-airflow-providers-google` | Запуск запросов и операций BigQuery |
| Redshift | Колоночное MPP-хранилище | `apache-airflow-providers-amazon` | SQL или Redshift Data API |
| Kafka | Event-streaming платформа | `apache-airflow-providers-apache-kafka` | Чтение, публикация и ожидание сообщений |
| Spark | Распределённый движок обработки | `apache-airflow-providers-apache-spark` | Запуск Spark jobs |

Для PostgreSQL, HTTP API и SMTP также существуют готовые provider-пакеты.

## Поддержка требований

| Требование | Реализация в Airflow |
| --- | --- |
| Ветвление и условия | Branch tasks, ShortCircuit и trigger rules |
| Event triggers | Sensors, assets, watchers и внешние triggers |
| Retry | Количество попыток, задержка и exponential backoff для каждой task |
| Fallback logic | Альтернативные ветки, trigger rules и error callbacks |
| Email | SMTP callbacks при успехе, ошибке или retry |
| Мониторинг | Web UI, история запусков, состояния tasks и логи |

## Развёртывание

POC запускается локально через Docker Compose. Для production предпочтителен managed Airflow, например Google Cloud Composer или Amazon MWAA: облачный провайдер обслуживает основные компоненты оркестратора. Kubernetes с официальным Helm chart подходит, если важнее переносимость и полный контроль.

Airflow должен оркестрировать обработку, а ресурсоёмкие вычисления для больших объёмов — передавать внешнему движку, например Spark.
