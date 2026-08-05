# Distributed Scheduling: k8s CronJob для экспорта данных

Ежедневная выгрузка данных в 20:00 для аналитиков (кейс грузоперевозок). Для демонстрации реализован экспорт одной таблицы — `shipments` (перевозки), остальные (`shipment_events`, `drivers`, `vehicles`, `clients`) экспортируются тем же способом при необходимости.

Схема таблицы `shipments` в задании не дана, поэтому для демонстрации взята минимальная правдоподобная структура: `id, client_id, driver_id, vehicle_id, status, created_at` — см. `k8s/postgres-init-configmap.yaml`.

Приложение экспорта — консольное на .NET 10 (`export-app/`), подключается к PostgreSQL через Npgsql 10 и Dapper, выгружает `shipments` в CSV.

## Структура

- `export-app/` — `Program.cs`, `ExportApp.csproj`, `Dockerfile`, `Properties/launchSettings.json` (переменные окружения для запуска из VS/`dotnet run`), `local/` (docker-compose с тестовым Postgres для локальной отладки)
- `k8s/postgres-init-configmap.yaml`, `k8s/postgres.yaml` — тестовый Postgres в кластере с таблицей `shipments` и несколькими строками (только для демонстрации; в реальной инфраструктуре экспорт шёл бы к уже существующей БД перевозок)
- `k8s/export-secret.yaml` — строка подключения для приложения экспорта (с `Gss Encryption Mode=Disable`, чтобы Npgsql не пытался искать отсутствующую в минимальном runtime-образе библиотеку `libgssapi_krb5`)
- `k8s/export-cronjob.yaml` — сам CronJob (`schedule: "0 20 * * *"`) + PVC для результата
- `k8s/debug-pod.yaml` — вспомогательный под для проверки содержимого PVC с результатом (сам CronJob не оставляет живых подов для `kubectl exec`)

## Как развернуть и проверить в MiniKube

1. Запустить MiniKube:
   ```bash
   minikube start
   ```

2. Собрать образ приложения экспорта в Docker-демоне MiniKube, чтобы не публиковать в реестр:
   ```bash
   eval $(minikube docker-env)
   cd task-3/results/export-app
   docker build -t shipments-export:local .
   cd ..
   ```

3. Развернуть Postgres с тестовыми данными и дождаться готовности:
   ```bash
   kubectl apply -f k8s/postgres-init-configmap.yaml
   kubectl apply -f k8s/postgres.yaml
   kubectl wait --for=condition=available deployment/postgres --timeout=120s
   ```

4. Развернуть секрет и CronJob:
   ```bash
   kubectl apply -f k8s/export-secret.yaml
   kubectl apply -f k8s/export-cronjob.yaml
   ```

5. Не дожидаясь 20:00 — запустить разовый прогон вручную:
   ```bash
   kubectl create job --from=cronjob/shipments-export manual-test-1
   kubectl get pods -w
   ```
   Дождаться статуса `Completed`, затем посмотреть лог:
   ```bash
   kubectl logs job/manual-test-1
   ```

6. Проверить содержимое CSV через вспомогательный под:
   ```bash
   kubectl apply -f k8s/debug-pod.yaml
   kubectl exec export-output-viewer -- cat /export/shipments.csv
   ```

7. Убедиться, что CronJob зарегистрирован с нужным расписанием:
   ```bash
   kubectl get cronjob shipments-export
   ```
