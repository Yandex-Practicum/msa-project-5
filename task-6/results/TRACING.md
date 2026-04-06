# Обоснование решений по трейсингу

## Что изменилось по сравнению с Task 5

| Аспект     | Task 5                                  | Task 6                                                      |
| ---------- | --------------------------------------- | ----------------------------------------------------------- |
| Запуск Job | При старте приложения                   | По REST API `POST /api/jobs/import-products`                |
| Трейсинг   | Только `traceId`/`spanId` в логах (MDC) | Полноценный distributed tracing: клиент -> API -> Batch Job |
| Zipkin     | Нет                                     | Визуализация трейсов в Zipkin UI                            |
| Клиент     | Нет                                     | Python-клиент с B3 Propagation                              |

## Архитектура трейсинга

C4-диаграмма: [c4_tracing.puml](c4_tracing.puml)

## Выбор решения: Micrometer Tracing + Brave + Zipkin

### Почему Micrometer Tracing, а не OpenTelemetry SDK напрямую

| Критерий                    |         Micrometer Tracing          |             OpenTelemetry SDK              |
| --------------------------- | :---------------------------------: | :----------------------------------------: |
| Интеграция с Spring Boot 3  |    Нативная (auto-configuration)    | Требует Java Agent или ручной конфигурации |
| Spring Batch инструментация |   Автоматическая (Job/Step spans)   |            Нужно писать вручную            |
| Конфигурация                | 2 строки в `application.properties` |      Переменные окружения + Agent JAR      |
| Переключение бэкенда        | Замена зависимости (Brave -> OTel)  |         Привязка к OTel Collector          |
| Overhead                    |             Минимальный             |    Java Agent увеличивает startup time     |

Spring Boot 3 использует Micrometer Tracing как абстракцию. Brave — реализация, которая отправляет spans в Zipkin. Если в будущем нужно перейти на Jaeger/Tempo — достаточно заменить зависимость на `micrometer-tracing-bridge-otel`.

### Почему Zipkin, а не Jaeger

1. Простота — один Docker-образ без дополнительных зависимостей (in-memory storage для POC).
2. Нативная интеграция — `zipkin-reporter-brave` — стандартный reporter для Brave.
3. UI — встроенный UI для поиска трейсов по traceId, сервису, тегам.

Для продакшена рекомендуется Jaeger или Grafana Tempo с persistent storage.

### Почему B3 Propagation в клиенте

B3 — стандартный формат пропагации контекста, поддерживаемый Brave/Zipkin из коробки. Клиент передает заголовок `b3: {traceId}-{spanId}-1`, и Spring Boot автоматически подхватывает его, создавая дочерний span с тем же traceId.

Это обеспечивает сквозной трейсинг: один traceId виден и в логах клиента, и в логах приложения, и в Zipkin UI.

## Что видно в логах (ELK)

Каждая JSON-строка лога содержит:

```json
{
  "trace": "123qwe...",
  "span": "123qwe...",
  "app": "batch-processing",
  "level": "INFO",
  "msg": "Received request to launch importProductJob",
  "logger": "com.example.batchprocessing.JobController",
  "thread": "http-nio-8080-exec-1"
}
```

В Kibana можно фильтровать по `trace` для просмотра всех событий одного запроса — от вызова API до завершения Batch Job.
