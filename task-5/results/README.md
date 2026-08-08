# Task 5 — Мониторинг, оповещение и логирование ETL-приложения

Развитие решения Task 4: Spring Batch-приложение оснащено метриками (Prometheus/Grafana)
и централизованным сбором логов (ELK), настроены алерты.

## Состав

- `diagrams/c4-monitoring.puml` — C4-диаграмма с контуром наблюдаемости.
- `METRICS.md` — какие метрики выбраны и почему; обоснование Actuator+Micrometer против JMX Exporter.
- `LOGGING-AND-ALERTING.md` — обоснование конвейера логов (stdout → Filebeat → Logstash → Elasticsearch) и выбора Prometheus Alert Rules против Grafana Alerting.
- `src/` — приложение из Task 4 + изменения:
  - `spring-boot-starter-web` + `spring-boot-starter-actuator` + `micrometer-registry-prometheus` — эндпоинт `/actuator/prometheus`;
  - `JobCompletionNotificationListener` публикует бизнес-метрики `etl_job_runs_total`, `etl_records_*`, `etl_job_duration_seconds`;
  - `logback-spring.xml` — структурированные JSON-логи в stdout;
  - приложение остаётся запущенным после джобы (убран `System.exit`), чтобы Prometheus мог скрейпить метрики.
- `docker-compose.yml` — PostgreSQL, app, Prometheus, Grafana, Elasticsearch, Logstash, Kibana, Filebeat.
- `prometheus/prometheus.yml` + `prometheus/alert-rules.yml` — scrape-конфиг и алерты `EtlJobFailed`, `BatchAppDown`, `HighHeapUsage`.
- `grafana/provisioning/` — datasource Prometheus и дашборд «ETL Batch Processing — Overview» (8 панелей: запуски джоб, read/written, длительность, CPU, heap, потоки, GC, HTTP).
- `filebeat/`, `logstash/`, `init-db/` — конфигурации конвейера логов и схемы БД.
- `run-and-screenshots-plan.md` — пошаговый запуск с точками для скриншотов.

## Архитектура наблюдаемости

```
app ──/actuator/prometheus──> Prometheus ──> Grafana (дашборды, алерты)
app ──JSON-логи в /var/log/etl (общий volume)──> Filebeat ──> Logstash ──> Elasticsearch ──> Kibana
```

## Быстрый старт

```bash
cd task-5/results
./gradlew build -x test
docker compose up --build -d
```

- Grafana: http://localhost:3000 (admin/admin) → дашборд «ETL Batch Processing — Overview»
- Prometheus: http://localhost:9090 → Status → Targets, Alerts
- Kibana: http://localhost:5601 → index pattern `filebeat-*`
