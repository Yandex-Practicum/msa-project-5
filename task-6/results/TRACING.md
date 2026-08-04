# Трейсинг запуска ETL-джобы

## Выбранное решение

**Micrometer Tracing (мост Brave) + Zipkin + B3-пропагация + MDC в JSON-логах.**

| Компонент | Выбор | Почему |
|---|---|---|
| Инструментация приложения | `micrometer-tracing-bridge-brave` | Нативный для Spring Boot 3: автоматически создаёт спаны для входящих HTTP-запросов и кладёт `traceId`/`spanId` в MDC — код приложения не меняется |
| Хранилище и UI трейсов | Zipkin (`openzipkin/zipkin:3`) | Легковесный, один контейнер без БД, стандартный репортер `zipkin-reporter-brave`; для POC достаточно, в облаке заменяется на GCP Cloud Trace / Jaeger без изменения кода приложения |
| Протокол передачи контекста | B3 single-header (`b3: traceId-spanId-1`) | Нативный формат Brave; клиенту достаточно сгенерировать hex-идентификаторы — не нужен SDK |
| Связь логов и трейсов | MDC → `logstash-logback-encoder` | `traceId`/`spanId` из MDC попадают в каждое JSON-событие, `uri`/`httpMethod` добавляет `RequestTracingFilter` |

Альтернативы: OpenTelemetry bridge вместо Brave (равноценно; Brave выбран как самый короткий путь для B3 и Zipkin), Jaeger вместо Zipkin (тяжелее для локального POC, нужен OTLP-коллектор для полной функциональности).

## Как это работает

1. **Клиент** (`client/client.py`, Python) генерирует `traceId` (128-бит hex) и `spanId` (64-бит hex), отправляет `POST /api/etl/run` с заголовком `b3: {traceId}-{spanId}-1` и логирует вызов с теми же идентификаторами.
2. **Приложение** принимает заголовок, Brave продолжает трейс: входящий HTTP-спан получает переданный `traceId`. Спан отправляется в Zipkin (`http://zipkin:9411`).
3. **Контроллер** `JobLaunchController` запускает джобу через `JobLauncher` (автозапуск при старте отключён: `spring.batch.job.enabled=false`). Логи внутри запроса автоматически содержат `traceId`/`spanId` из MDC.
4. **RequestTracingFilter** добавляет в MDC `uri` и `httpMethod` запроса — требование «у контекста запроса должно быть traceId, spanId, URI» выполняется.
5. **Логи** приложения в JSON пишутся в файл на общем volume и уходят через Filebeat → Logstash → Elasticsearch; в Kibana запрос `traceId : "<id>"` показывает все события конкретного запуска джобы. Логи клиента (с тем же traceId) доступны через `docker compose logs client`.

## Пример события в Elasticsearch

```json
{
  "ts": "2026-08-06T07:30:12.345Z",
  "level": "INFO",
  "logger": "com.example.batchprocessing.JobLaunchController",
  "msg": "Запрошен запуск ETL-джобы importProductJob через API",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "uri": "/api/etl/run",
  "httpMethod": "POST",
  "app": "batch-processing"
}
```

## Почему не только логи

Трейсинг в Zipkin даёт длительность каждого спана (HTTP-запрос, работа джобы), а корреляция по `traceId` в ELK — детальные сообщения. Вместе они закрывают боль кейса TradeWare: «в случае ошибок трудно отследить причину и свести логи в одну картину».
