# Task 6: Трейсинг запросов Spring Batch

## Содержимое

- [TRACING.md](TRACING.md) - обоснование выбора решений по трейсингу
- [c4_tracing.puml](c4_tracing.puml) - C4-диаграмма архитектуры трейсинга
- REST API для запуска ETL-задачи (`POST /api/jobs/import-products`)
- Python-клиент с B3 Propagation для сквозного трейсинга
- Zipkin для визуализации распределенных трейсов
- ELK-стек для логов с traceId/spanId

## Запуск

```bash
cd task-6/results
docker compose up --build
```

### Доступные UI

| Сервис              | URL                                       | Логин         |
| ------------------- | ----------------------------------------- | ------------- |
| Zipkin              | http://localhost:9411                     | -             |
| Kibana              | http://localhost:5601                     | -             |
| Grafana             | http://localhost:3000                     | admin / admin |
| Prometheus          | http://localhost:9090                     | -             |
| Actuator Prometheus | http://localhost:8080/actuator/prometheus | -             |

## Как это работает

1. Приложение стартует и ждет HTTP-запросов (job НЕ запускается автоматически)
2. Python-клиент (`etl-client`) ждет healthcheck приложения, затем отправляет `POST /api/jobs/import-products` с B3-заголовком
3. Spring Boot подхватывает traceId из заголовка, создает дочерний span
4. Все логи содержат traceId и spanId (JSON structured logging)
5. Spans экспортируются в Zipkin

## Просмотр трейсов в Zipkin

1. Открыть http://localhost:9411
2. Нажать "Run Query" - видим трейс запроса
3. Кликнуть на трейс - видим spans: HTTP request -> Job execution

## Просмотр логов в Kibana

1. Открыть http://localhost:5601
2. Management -> Stack Management -> Index Patterns -> создать `filebeat-*`
3. Discover -> добавить колонки: `trace`, `span`, `level`, `msg`, `logger`
4. Фильтр: `app: "batch-processing" and trace: *` - логи с traceId

## Ручной запуск Job (повторный)

```bash
curl -X POST http://localhost:8080/api/jobs/import-products
```

Каждый вызов создает новый Job execution с уникальным traceId.

## Перезапуск с чистой БД

```bash
docker compose down -v
docker compose up --build
```
