# Task 5: Мониторинг, оповещение и логирование Spring Batch

## Содержимое

- [METRICS.md](METRICS.md) - обоснование выбора метрик и способов отправки логов
- [c4_monitoring.puml](c4_monitoring.puml) - C4-диаграмма архитектуры мониторинга и логирования
- Доработанное Spring Batch приложение с метриками и JSON-логированием
- Docker Compose с Prometheus, Grafana, ELK-стеком
- Grafana-дашборд с 8 панелями
- Prometheus alert rules (2 алерта)

## Запуск

```bash
cd task-5/results
docker compose up --build
```

### Доступные UI

| Сервис              | URL                                       | Логин         |
| ------------------- | ----------------------------------------- | ------------- |
| Grafana             | http://localhost:3000                     | admin / admin |
| Prometheus          | http://localhost:9090                     | -             |
| Kibana              | http://localhost:5601                     | -             |
| Actuator Prometheus | http://localhost:8080/actuator/prometheus | -             |

## Метрики (Prometheus + Grafana)

Приложение экспортирует метрики через Spring Boot Actuator + Micrometer на `/actuator/prometheus`. Prometheus скрейпит их каждые 5 секунд.

Grafana-дашборд содержит панели:

1. App Status (UP/DOWN)
2. CPU Usage (%)
3. JVM Heap Memory (bytes)
4. Live Threads
5. CPU Usage Over Time (graph)
6. JVM Heap Memory Over Time (graph)
7. HTTP Request Rate
8. GC Pause Time

## Оповещения (Prometheus Alert Rules)

Настроены в `prometheus/alert-rules.yml`:

1. BatchJobFailed - срабатывает при статусе FAILED у Spring Batch Job
2. HighCpuUsage - CPU > 80% в течение 1 минуты
3. AppDown - приложение недоступно более 30 секунд

Проверить в Prometheus UI: http://localhost:9090/alerts

## Логирование (ELK)

Цепочка: App (JSON logs) -> Docker -> Filebeat -> Logstash -> Elasticsearch -> Kibana

### Просмотр логов в Kibana

1. Открыть http://localhost:5601
2. Перейти в Management -> Stack Management -> Index Patterns
3. Создать index pattern: `filebeat-*`
4. Перейти в Discover - логи приложения доступны с полями `msg`, `level`, `logger`, `trace`

## Перезапуск с чистой БД

```bash
docker compose down -v
docker compose up --build
```
