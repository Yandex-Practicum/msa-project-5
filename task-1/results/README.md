# Task 1 — результаты

- `01-Обоснование-Airflow.md` — текстовое обоснование и таблица интеграций.
- POC: см. каталог **`../airflow`** (`docker-compose.yml`, `dags/poc_sprint5_batch.py`).

## Запуск на Windows

1. Установите [Docker Desktop](https://www.docker.com/products/docker-desktop/).
2. В PowerShell:
   ```powershell
   cd task-1\airflow
   docker compose up --build
   ```
3. Откройте Airflow UI: http://localhost:8080 (логин/пароль выводятся в лог контейнера `airflow` при первом старте `airflow standalone`).
4. Включите DAG `poc_sprint5_batch`, запустите вручную (Trigger DAG).
5. Письма смотрите в MailHog: http://localhost:8025
