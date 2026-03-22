# Task 6 — результаты

- API запуска job: `POST /api/jobs/run` (см. [`../initial/src/main/java/.../JobController.java`](../initial/src/main/java/com/example/batchprocessing/JobController.java)).
- **traceId / spanId** — Micrometer Tracing (Brave) + поле **requestUri** в MDC через `RequestMdcFilter`.
- Клиент: `client.py` — `python client.py` при запущенном приложении на `localhost:8080` (по умолчанию `127.0.0.1:8080`; переменная `BATCH_API_BASE` при необходимости). Клиент сначала проверяет `/actuator/health`, затем шлёт `POST` **без тела** — так обходится обрыв соединения (`RemoteDisconnected`) из‑за `Expect: 100-continue` у `urllib` с телом `b"{}"`.

Код приложения: [`../initial`](../initial). Логи в Kibana — после `docker compose up` в стеке task-6 (как в task-5).

## Запуск

1. Поднять стек: `cd task-6\initial`, `docker compose up --build`.
2. Применить `schema-all.sql` к PostgreSQL.
3. Вызвать `python task-6\results\client.py`.
4. В Kibana найти JSON-логи с полями trace/span (из logback) и `requestUri` в MDC.
