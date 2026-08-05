# Мониторинг, логирование и оповещение для Spring Batch ETL

К готовому Spring Batch-приложению добавлены Prometheus, Grafana (метрики + алерты) и ELK-стек (централизованные логи).

## Структура

- `c4-diagram.puml` — C4-диаграмма контейнеров To Be из задания 4, дополненная слоем «Мониторинг и логирование» (Prometheus, Grafana, Filebeat, Logstash, Elasticsearch, Kibana)
- `monitoring-logging-alerting.md` — обоснование выбранных метрик, способа доставки логов и конфигурации алертов
- `batch-processing-app/` — доработанное приложение из задания 4 + Docker Compose с полным стеком (Postgres, Elasticsearch, Logstash, Kibana, Filebeat, Prometheus, Grafana, само приложение)
- `screenshots/` — демонстрация: дашборд Grafana, алерты, логи в Kibana

## Что доработано в `batch-processing-app/` относительно шаблона задания

Помимо тех же правок, что и в задании 4 (заполненные `//todo` в `BatchConfiguration`/`ProductItemProcessor`/`JobCompletionNotificationListener`, защита от SQL-инъекции, фикс двойного `PRIMARY KEY` в `schema-all.sql`, `Dockerfile` с рабочим базовым образом):

- **`BatchProcessingApplication.java`** — убран `System.exit(SpringApplication.exit(...))`. В шаблоне задания 4 это была консольная утилита, которая завершалась сразу после Job'а. Здесь так нельзя: Prometheus скрейпит метрики по HTTP каждые 5 секунд, а скрейпить не с чего, если процесс уже мёртв. Job по-прежнему выполняется один раз при старте, но приложение остаётся жить как обычный сервис.
- **`build.gradle`** — добавлены `spring-boot-starter-actuator` (без него `/actuator/prometheus` не существует) и `spring-boot-starter-web` (embedded-сервер, без которого actuator не может отдавать эндпойнты по HTTP и нечему держать процесс живым); убрана лишняя зависимость `com.mysql:mysql-connector-j` (проект использует PostgreSQL).
- **`application.properties`** — `logging.level.com.ivanfranchin.moviesapi=DEBUG` (чужой пакет из другого примера, не соответствовал ничему в проекте) заменён на реальный пакет `com.example.batchprocessing`.
- **`logback-spring.xml`** — аналогично, `<logger name="batch-processing">` заменён на `<logger name="com.example.batchprocessing">` — иначе явное правило DEBUG-логирования было «мёртвым» и не срабатывало ни для одного реального логгера.
- **`docker-compose.yml`** — опечатка `depends_on: filebeat1` (сервис называется `filebeat`) мешала бы `docker compose up` запуститься; добавлен `ports: 8080:8080` для `app`.
- **`prometheus/prometheus.yml`** — таргет скрейпа сменён с `host.docker.internal:8080` на `app:8080` (имя сервиса в той же docker-compose сети — надёжнее, чем полагаться на DNS-хак Docker Desktop).
- **`grafana/provisioning/dashboards/batch-processing-dashboard.json`** — дашборд дополнен двумя панелями (`JVM Heap Used`, `Job Executions by Status`) сверх изначальной панели `CPU`.
- Настроены два alert-правила в Grafana Alerting (`App Down`, `Batch Job Failed`), правила живут в БД Grafana, см. обоснование и скриншот в `monitoring-logging-alerting.md`.

## Как развернуть и проверить локально

Понадобится Docker Desktop и JDK 17–21.

1. Собрать jar и образы:
   ```bash
   cd batch-processing-app
   ./gradlew build -x test
   docker compose build app filebeat
   ```

2. Поднять весь стек:
   ```bash
   docker compose up -d
   ```

3. Создать таблицы и загрузить данные программы лояльности (приложение читает `loyality_data`, само её не наполняет):
   ```bash
   docker cp src/main/resources/schema-all.sql batch-processing-app-postgresdb-1:/schema-all.sql
   docker exec batch-processing-app-postgresdb-1 psql -U postgres -d productsdb -f /schema-all.sql
   docker cp src/main/resources/loyality_data.csv batch-processing-app-postgresdb-1:/loyality_data.csv
   docker exec batch-processing-app-postgresdb-1 psql -U postgres -d productsdb -c "\copy loyality_data(productSku, loyalityData) FROM '/loyality_data.csv' WITH (FORMAT csv)"
   ```

4. Проверить:
   - Метрики: `curl http://localhost:8080/actuator/prometheus`
   - Prometheus видит таргет: `http://localhost:9090/targets` (должен быть `UP`)
   - Grafana: `http://localhost:3000` (admin/admin) → Dashboards → batch-processing-dashboard
   - Алерты: `http://localhost:3000/alerting/list`
   - Kibana: `http://localhost:5601` → Discover → data view `filebeat-*`, фильтр `container.name: "app"`


## Скриншоты (`screenshots/`)

- `grafana-dashboard.png` — дашборд с тремя панелями: CPU, JVM Heap Used, Job Executions by Status
- `grafana-alert-rules.png` — оба alert-правила (`App Down`, `Batch Job Failed`) в состоянии `Normal`
- `kibana-logs.png` — Discover в Kibana, отфильтровано по `container.name: "app"`, структурированные поля лога (`level`, `logger`, `msg`, `class`)
