# Task 5

Локальный POC для мониторинга, логирования и оповещений вокруг Spring Batch-приложения.

Что входит в решение:

- `initial/` - готовое приложение и docker-compose стек с PostgreSQL, Prometheus, Grafana, Elasticsearch, Logstash, Kibana и Filebeat
- `C4.puml` - обновлённая контейнерная C4-диаграмма
- `results/README.md` - краткое описание решения и сценарий проверки
- `results/METRICS.md` - обоснование выбранных метрик
- `results/LOGGING_AND_ALERTING.md` - обоснование логирования и alerting-подхода

## Запуск

Из директории `task-5/initial`:

```bash
docker compose up --build
```

После старта сервисы доступны так:

- Grafana: `http://localhost:3000` (`admin/admin`)
- Prometheus: `http://localhost:9090`
- Kibana: `http://localhost:5601`
- Spring Batch app health: `http://localhost:8080/actuator/health`
- JMX Exporter metrics: `http://localhost:9404/metrics`

## Что проверить

1. В Grafana автоматически появляется дашборд `Batch Processing Observability`.
2. В Prometheus видны метрики `batch_job_last_*` и активны правила из `prometheus/alerts.yml`.
3. В Elasticsearch создаются индексы `batch-processing-*`.
4. В Kibana можно создать data view `batch-processing-*` и увидеть JSON-логи batch-прогона.

## Примечания

- Приложение пишет структурированные JSON-логи в общий volume, откуда их забирает Filebeat.
- Метрики экспортируются через `jmx_prometheus_javaagent` на порту `9404`.
- Для batch-приложения выбраны доменные метрики последнего запуска, а не только инфраструктурные JVM-счётчики.
