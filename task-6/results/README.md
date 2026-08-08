# Task 6 — API запуска ETL-джобы со сквозным трейсингом

Развитие решения Task 5: джоба больше не запускается при старте приложения —
её запускает REST-вызов, а каждый запуск сопровождается сквозным трейсингом
(traceId, spanId, URI) в логах и Zipkin.

## Что изменилось относительно Task 5

- `JobLaunchController` — REST API:
  - `POST /api/etl/run` — очистка `products` и запуск `importProductJob` с уникальными параметрами;
  - `GET /api/etl/status` — проверка живости сервиса.
- `spring.batch.job.enabled=false` — автозапуск джобы отключён.
- `RequestTracingFilter` — кладёт `uri` и `httpMethod` запроса в MDC; `traceId`/`spanId` добавляет Micrometer Tracing (Brave).
- Зависимости `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave`; спаны отправляются в Zipkin (`management.zipkin.tracing.endpoint`).
- `client/` — Python-клиент: ждёт готовности приложения, генерирует traceId/spanId, вызывает API с B3-заголовком, логирует вызов с тем же traceId.
- В `docker-compose.yml` добавлены сервисы `zipkin` и `client`; стек мониторинга и логирования из Task 5 сохранён полностью.

## Состав

- `TRACING.md` — обоснование выбора решения для трейсинга (Brave + Zipkin + B3) и схема работы.
- `diagrams/c4-tracing.puml` — C4-диаграмма решения.
- `src/`, `client/`, `docker-compose.yml`, `prometheus/`, `grafana/`, `filebeat/`, `logstash/`, `init-db/` — приложение и инфраструктура.
- `run-and-screenshots-plan.md` — пошаговый запуск с точками для скриншотов.

## Быстрый старт

```bash
cd task-6/results
./gradlew build -x test
docker compose up --build -d        # клиент сам дождётся app и вызовет API
docker compose logs client          # traceId запуска
docker compose logs app | grep traceId
```

- Zipkin: http://localhost:9411 → Find Traces
- Kibana: http://localhost:5601 → Discover → `filebeat-*` → фильтр `traceId : "<id>"`
- Grafana: http://localhost:3000, Prometheus: http://localhost:9090
