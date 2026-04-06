# Task 2: Дизайн модуля пакетной обработки данных

## Обоснование выбора технологического решения

### Сравнительная таблица

| Критерий | Spring Batch | Apache Airflow | K8s CronJob | Spark |
|----------|:---:|:---:|:---:|:---:|
| CRON-расписание | Через Spring Scheduler или внешний триггер | Встроенный CRON в DAG | Нативный CronJob-манифест | Нет из коробки, требуется внешний оркестратор |
| Сложность реализации логики | Средняя: требуется описать Step, Reader, Writer, конфигурацию Job | Средняя: DAG + PythonOperator/PostgresOperator | Низкая: простой скрипт на Python/SQL, упакованный в контейнер | Высокая: избыточный фреймворк для простого JOIN + export |
| Ресурсоемкость | Средняя: JVM потребляет 256-512 MB RAM даже для простых задач | Высокая: Scheduler + Webserver + Metadata DB = 1-2 GB RAM постоянно | Минимальная: Pod запускается по расписанию, потребляет ресурсы только во время выполнения, затем удаляется | Очень высокая: Driver + Executor, минимум 1-2 GB для запуска кластера |
| Масштабируемость и сложность | Средняя: партиционирование через Spring Batch, но требует доработки | Средняя: параллелизм через воркеры, но избыточно для данной задачи | Высокая при низкой сложности: горизонтальное масштабирование через параллельные CronJob-ы или увеличение ресурсов Pod | Высокая: нативная параллельная обработка, но не востребована при 10K строк |
| Развертывание в облаке и интеграция с микросервисами | Средняя: деплоится как отдельный микросервис, нужен Docker-образ и Deployment | Сложная: требуется отдельный кластер Airflow (Composer/Helm) поверх K8s | Нативная: CronJob - часть K8s API, не требует дополнительной инфраструктуры, использует те же инструменты CI/CD | Сложная: требуется Spark-кластер (Spark Operator / Dataproc) |
| Логирование и мониторинг | Хорошая: Spring Boot Actuator, Micrometer, интеграция с Prometheus/Grafana | Отличная: встроенный UI, метрики, история запусков | Хорошая: стандартные логи K8s (kubectl logs), интеграция с существующим стеком мониторинга (Prometheus, Loki, Grafana) | Средняя: Spark UI, но требует дополнительной настройки для интеграции |

### Вывод: K8s CronJob

Для данной задачи выбран Kubernetes CronJob:

1. Простота задачи соответствует простоте решения.Задача сводится к SQL JOIN четырех таблиц и записи результата в CSV. Нет сложной бизнес-логики, трансформаций или ветвлений. K8s CronJob позволяет решить это одним скриптом в контейнере без дополнительных фреймворков.

2. Минимальная ресурсоемкость. Pod создается строго по расписанию, выполняет работу и удаляется. Нет постоянно работающих компонентов (в отличие от Airflow с его Scheduler/Webserver или JVM Spring Batch). При объеме 10K строк достаточно 128-256 MB RAM.

3. Нативная интеграция с инфраструктурой. Микросервисы магазина уже работают в K8s. CronJob использует тот же кластер, те же секреты для подключения к БД, тот же CI/CD пайплайн, те же инструменты мониторинга. Не нужна дополнительная инфраструктура.

4. CRON из коробки. K8s CronJob нативно поддерживает cron-расписание (`schedule: "0 6 * * *"`), retry-политику (`backoffLimit`), историю запусков (`successfulJobsHistoryLimit`) и конкурентность (`concurrencyPolicy`).

5. Масштабируемость при росте. Если количество клиентов вырастет, можно параллелизировать генерацию: запускать несколько Pod-ов для разных сегментов клиентов. При кардинальном росте сложности (ветвления, зависимости, множество шагов) можно мигрировать на Airflow с `KubernetesPodOperator`, не переписывая сам скрипт генерации.

### Почему не другие решения

- Apache Airflow — избыточен. Постоянно работающий кластер (Scheduler + Webserver + Metadata DB) для одного ежедневного задания без ветвлений и зависимостей — неоправданный расход ресурсов и сложности.
- Spring Batch — избыточен для простого SQL-экспорта. Требует JVM, Spring-конфигурации, описания Job/Step/Reader/Writer. Оправдан при сложной бизнес-логике обработки, которой здесь нет.
- Spark — совсем избыточен. Распределенный движок для обработки 10K строк — ту мач для такой задачи.

---

## C4-диаграмма контекста To Be

Диаграмма в формате PlantUML: [c4_to_be.puml](c4_to_be.puml)

### Описание работы решения

1. Каждый день в 6:00 Kubernetes создает Pod на основе CronJob-манифеста.
2. Pod подключается к PostgreSQL, используя секреты K8s для credentials.
3. Выполняется SQL-запрос с JOIN таблиц `products`, `categories`, `clients`, `client_prices` для формирования индивидуальных прайс-листов.
4. Результат записывается в CSV-файлы (по одному на каждого B2B-клиента) и загружается в файловое хранилище (PVC, S3 или GCS).
5. Pod завершается с exit code 0 (успех) или 1 (ошибка).
6. Prometheus собирает метрики CronJob (kube-state-metrics), Grafana отображает дашборд. При ошибке срабатывает алерт.

---

## Верхнеуровневый план имплементации

### Этап 1: Подготовка скрипта генерации

1. Написать Python-скрипт `generate_pricelists.py`:
  - Подключение к PostgreSQL через `psycopg2`
  - SQL-запрос с JOIN четырех таблиц:
   ```sql
   SELECT c.client_name, p.product_name, cat.category_name,
      cp.custom_price, p.base_price
   FROM client_prices cp
   JOIN clients c ON c.id = cp.client_id
   JOIN products p ON p.id = cp.product_id
   JOIN categories cat ON cat.id = p.category_id
   WHERE c.is_active = true
   ORDER BY c.client_name, cat.category_name, p.product_name;
   ```
  - Генерация CSV-файлов (один файл на клиента) через модуль `csv`
  - Загрузка файлов в хранилище (PVC или S3/GCS через SDK)
  - Логирование: structured JSON logs для интеграции с Loki/ELK

2. Покрыть скрипт unit-тестами.

### Этап 2: Контейнеризация

1. Создать `Dockerfile`:
  ```dockerfile
  FROM python:3.12-slim
  WORKDIR /app
  COPY requirements.txt .
  RUN pip install --no-cache-dir -r requirements.txt
  COPY generate_pricelists.py .
  CMD ["python", "generate_pricelists.py"]
  ```
2. Собрать и протестировать образ локально.
3. Загрузить образ в Container Registry (GCR / Docker Hub).

### Этап 3: Конфигурация K8s CronJob

1. Создать манифест `cronjob.yaml`:
  ```yaml
  apiVersion: batch/v1
  kind: CronJob
  metadata:
   name: price-list-generator
  spec:
   schedule: "0 6 * * *"
   concurrencyPolicy: Forbid
   successfulJobsHistoryLimit: 7
   failedJobsHistoryLimit: 3
   jobTemplate:
    spec:
     backoffLimit: 3
     activeDeadlineSeconds: 600
     template:
      spec:
       containers:
       - name: price-list-gen
        image: registry/price-list-gen:latest
        resources:
         requests:
          memory: "128Mi"
          cpu: "100m"
         limits:
          memory: "256Mi"
          cpu: "500m"
        envFrom:
        - secretRef:
          name: pricelist-db-credentials
        volumeMounts:
        - name: output
         mountPath: /output
       volumes:
       - name: output
        persistentVolumeClaim:
         claimName: pricelist-pvc
       restartPolicy: OnFailure
  ```

2. Создать Secret для подключения к БД:
  ```yaml
  apiVersion: v1
  kind: Secret
  metadata:
   name: pricelist-db-credentials
  type: Opaque
  stringData:
   DB_HOST: "postgres-service"
   DB_PORT: "5432"
   DB_NAME: "shop"
   DB_USER: "pricelist_reader"
   DB_PASSWORD: "<password>"
  ```

3. Создать PVC для хранения сгенерированных файлов (или настроить загрузку в S3/GCS).

### Этап 4: Мониторинг и алертинг

1. Убедиться, что `kube-state-metrics` собирает метрики CronJob:
  - `kube_job_status_succeeded` — успешные запуски
  - `kube_job_status_failed` — ошибки
  - `kube_job_duration_seconds` — время выполнения

2. Создать Grafana-дашборд с панелями:
  - Статус последнего запуска (успех/ошибка)
  - Время выполнения (тренд)
  - Количество сгенерированных файлов

3. Настроить алерт в Prometheus/Alertmanager:
  ```yaml
  - alert: PriceListGenerationFailed
   expr: kube_job_status_failed{job_name=~"price-list-generator.*"} > 0
   for: 5m
   labels:
    severity: warning
   annotations:
    summary: "Ошибка генерации прайс-листов"
  ```

### Этап 5: CI/CD и деплой

1. Добавить в CI/CD пайплайн:
  - Сборка Docker-образа при изменении скрипта
  - Прогон тестов
  - Push образа в Registry
  - `kubectl apply -f cronjob.yaml`

2. Провести тестовый запуск: `kubectl create job --from=cronjob/price-list-generator test-run`
3. Проверить логи: `kubectl logs job/test-run`
4. Проверить сгенерированные CSV-файлы.
