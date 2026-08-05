# Трейсинг: запуск ETL-задачи по API

Spring Batch-задача больше не запускается при старте приложения, а поднимается по вызову REST-эндпойнта. Вызов сопровождается логированием с `traceId`/`spanId`/URI, клиент (на .NET 10) вызывает этот эндпойнт.

## Структура

- `batch-processing-app/` — доработанное приложение из задания 5
- `import-job-client/` — консольный клиент на .NET 10, вызывает эндпойнт запуска Job
- `screenshots/` — демонстрация: логи сервера с trace-контекстом, отображение в Kibana

## Что доработано в `batch-processing-app/` относительно задания 5

- **`application.properties`** — добавлена `spring.batch.job.enabled=false` (отключает автозапуск Job'а раннером `JobLauncherApplicationRunner` при старте контекста — сам бин `Job` остаётся зарегистрированным, просто больше никто не запускает его сам) и `management.tracing.sampling.probability=1.0` (100% сэмплинга — для демонстрации важно трассировать каждый вызов).
- **`build.gradle`** — мост трейсинга сменён с `micrometer-tracing-bridge-brave` на `micrometer-tracing-bridge-otel`.
- **`JobTriggerController.java`** (новый файл) — REST-контроллер с `POST /jobs/import-product`. Внутри:
  - логирует `URI` запроса (`traceId`/`spanId` в лог добавлять вручную не нужно — Micrometer Tracing автоматически оборачивает каждый HTTP-запрос в спан и кладёт trace-контекст в MDC ещё до того, как выполнение дойдёт до контроллера; `logback-spring.xml` из задания 5 уже настроен читать эти поля из MDC)
  - запускает Job через `JobLauncher.run(...)` с уникальным параметром `run.id` (текущий timestamp) на каждый вызов. 
  - ловит checked-исключения `JobLauncher.run()` и возвращает `500` с сообщением при сбое запуска

## `import-job-client/` — клиент на .NET 10

Консольное приложение на OpenTelemetry SDK: `OpenTelemetry`, `OpenTelemetry.Instrumentation.Http`, `OpenTelemetry.Exporter.Console`.

- `Sdk.CreateTracerProviderBuilder().AddSource("ImportJobClient")` — регистрирует наш `ActivitySource`, чтобы `StartActivity()` создавал спаны.
- `.AddHttpClientInstrumentation()` — автоматически создаёт **дочерний спан на сам HTTP-вызов** (виден как `POST` с `ParentSpanId`, равным `SpanId` родительского `trigger-import-job`), с готовыми тегами `http.request.method`, `url.full`, `http.response.status_code`. Также сам добавляет заголовок `traceparent` в исходящий запрос — тот же эффект, что и штатный `DiagnosticsHandler` в .NET, но с должной интеграцией в SDK.
- `.AddConsoleExporter()` — выводит оба спана (родительский и дочерний HTTP) в консоль в стандартном OTel-формате со всеми тегами и длительностью.
- `activity?.SetStatus(ActivityStatusCode.Error, ...)` — при сбое запроса явно помечает спан как ошибочный, а не просто ловит исключение.
- `HttpClient` шлёт `POST` на `JOB_ENDPOINT` (по умолчанию `http://localhost:8080/jobs/import-product`, настраивается через переменную окружения).

Путь к реальному backend-у трейсинга (Jaeger/Tempo) — замена `.AddConsoleExporter()` на `.AddOtlpExporter(...)`, без изменения остального кода.

**Сквозная трассировка клиент-сервер работает по-настоящему.** На сервере (`build.gradle`) мост трейсинга сменён с `micrometer-tracing-bridge-brave` на `micrometer-tracing-bridge-otel` — OTel-мост понимает W3C `traceparent` "из коробки" (у Brave по умолчанию B3), тем же форматом, что генерирует `AddHttpClientInstrumentation()` на клиенте. Один сквозной трейс на весь запрос.

## Как развернуть и проверить локально

Нужен Docker Desktop, JDK 17-21 для сборки Java-части, .NET 10 SDK для клиента.

1. Собрать и поднять сервер:
   ```bash
   cd batch-processing-app
   ./gradlew build -x test
   docker compose build app filebeat
   docker compose up -d
   ```

2. Создать схему БД и загрузить данные лояльности:
   ```bash
   docker cp src/main/resources/schema-all.sql batch-processing-app-postgresdb-1:/schema-all.sql
   docker exec batch-processing-app-postgresdb-1 psql -U postgres -d productsdb -f /schema-all.sql
   docker cp src/main/resources/loyality_data.csv batch-processing-app-postgresdb-1:/loyality_data.csv
   docker exec batch-processing-app-postgresdb-1 psql -U postgres -d productsdb -c "\copy loyality_data(productSku, loyalityData) FROM '/loyality_data.csv' WITH (FORMAT csv)"
   ```

3. Убедиться, что Job не стартовал сам:
   ```bash
   docker logs app
   ```
   Не должно быть строк `Job: [SimpleJob:...] launched...`.

4. Собрать и запустить клиент:
   ```bash
   cd ../import-job-client
   dotnet run
   ```

5. Проверить логи сервера:
   ```bash
   docker logs app --tail 30
   ```
   В JSON-строках `Received request to trigger Job, uri=/jobs/import-product` и `Job triggered, uri=..., status=COMPLETED` — непустые поля `trace`/`span`, совпадающие в паре этих строк.

6. Открыть Kibana (`http://localhost:5601`) → Discover → data view `filebeat-*` → фильтр `logger: "com.example.batchprocessing.JobTriggerController"`.