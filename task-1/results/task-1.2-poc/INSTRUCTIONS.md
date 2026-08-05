# Task 1.2. Локальный запуск

Команды выполняются из корня репозитория.

## 1. Запустить POC

Инициализировать Airflow:

```bash
docker compose \
  -f task-1/results/task-1.2-poc/docker-compose.yml \
  up airflow-init
```

Запустить сервисы:

```bash
docker compose \
  -f task-1/results/task-1.2-poc/docker-compose.yml \
  up -d
```

Проверить контейнеры:

```bash
docker compose \
  -f task-1/results/task-1.2-poc/docker-compose.yml \
  ps
```

Интерфейсы:

- Airflow: <http://localhost:8080>, логин и пароль — `airflow`;
- Mailpit: <http://localhost:8025>.

## 2. Проверить успешный сценарий

В Airflow запустить DAG `customer_delivery_batch` с параметрами по умолчанию.

Ожидаемый результат:

- выбрана ветка `process_large_batch`;
- `retry_demo` завершается со второй попытки;
- DAG получает статус `Success`;
- Mailpit получает письма `RETRY` и `SUCCESS`;
- JSON-отчёт создаётся в `task-1/results/task-1.2-poc/data/output`.

## 3. Проверить ошибочный сценарий

Запустить DAG с параметрами:

```json
{
  "force_failure": true,
  "large_batch_threshold": 5
}
```

Ожидаемый результат:

- `read_and_analyze_csv` завершается ошибкой;
- DAG получает статус `Failed`;
- Mailpit получает письмо `FAILED`.

## 4. Подготовить скриншоты

Сохранить в `task-1/results/task-1.2-poc/evidence`:

- список успешного и ошибочного запусков Airflow;
- задачи успешного запуска, включая повторную попытку `retry_demo`;
- уведомления `RETRY`, `SUCCESS` и `FAILED` в Mailpit.

## 5. Остановить POC

```bash
docker compose \
  -f task-1/results/task-1.2-poc/docker-compose.yml \
  down
```
