
# Реализация k8s CronJob  

## Технологический стек
- Язык: **C# 12 + .NET 10.0**
- Библиотека: **Npgsql** для работы с PostgreSQL
- Контейнеризация: **Docker**
- Оркестрация: **Kubernetes CronJob + Job** (Minikube)
- Сборка: Single-file self-contained executable

## Демонстрация работы

### 1. CronJob создан и активен
```log
NAME                SCHEDULE      TIMEZONE   SUSPEND   ACTIVE   LAST SCHEDULE   AGE
daily-data-export   */1 * * * *   <none>     False     1        48s             80s
```
### 2. Job завершился успешно
```log
NAME                            STATUS     COMPLETIONS   DURATION   AGE
daily-export-cronjob           Complete   1/1           1s         44s
```

### 3. Логи пода — успех!
```log
2026-02-25 20:40:18 [INF] Exported 0 clients to /tmp/export/clients.csv
2026-02-25 20:40:18 [INF] Export completed successfully. Total rows: 0
2026-02-25 20:40:18 [INF] ========================================
2026-02-25 20:40:18 [INF] Export completed successfully
2026-02-25 20:40:18 [INF] Total rows exported: 0
2026-02-25 20:40:18 [INF] Duration: 0.51 seconds
2026-02-25 20:40:18 [INF] ========================================
2026-02-26 19:27:02 [INF] ========================================
2026-02-26 19:27:02 [INF] Starting Transport Data Exporter
2026-02-26 19:27:02 [INF] Timestamp: 2026-02-26 19:27:02
2026-02-26 19:27:02 [INF] ========================================

```
