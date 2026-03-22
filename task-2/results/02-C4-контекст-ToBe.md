# C4 — уровень контекста (To Be)

## Текстовое описание

- **Пользователь / планировщик:** по расписанию (06:00) запускается **пакетная выгрузка** в Kubernetes.
- **Система выгрузки:** Job/CronJob выполняет запрос к **PostgreSQL** (связка `products`, `categories`, `clients`, `client_prices`), формирует **CSV** и сохраняет файл в **объектное хранилище** (S3-совместимое / корпоративное) или сетевую папку для B2B.
- **Наблюдаемость:** логи pod → **ELK**, метрики → **Prometheus/Grafana** 

## Диаграмма (Mermaid)

```mermaid
flowchart LR
  scheduler[K8s_CronJob]
  db[(PostgreSQL)]
  exp[Export_container]
  storage[File_storage_S3]
  ops[Logs_and_metrics]

  scheduler --> exp
  exp --> db
  exp --> storage
  exp --> ops
```

