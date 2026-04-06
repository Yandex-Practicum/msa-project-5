# Обоснование выбора метрик и решений по мониторингу и логированию

## Выбор метрик для мониторинга

### Метрики приложения (Spring Boot Actuator + Micrometer)

| Метрика                              | Источник                | Зачем                                                                                               |
| ------------------------------------ | ----------------------- | --------------------------------------------------------------------------------------------------- |
| `up`                                 | Prometheus target       | Доступность приложения. Базовый health-check                                                        |
| `process_cpu_usage`                  | JVM MXBean              | Потребление CPU. Критично для обнаружения перегрузки при пиковых объемах обработки                  |
| `jvm_memory_used_bytes{area="heap"}` | JVM MXBean              | Использование heap-памяти. При обработке 400K строк важно следить за утечками памяти и давлением GC |
| `jvm_memory_max_bytes{area="heap"}`  | JVM MXBean              | Максимальный размер heap. Для понимания, сколько запаса осталось                                    |
| `jvm_threads_live_threads`           | JVM MXBean              | Количество потоков. Рост может указывать на проблемы с пулом потоков или deadlock                   |
| `jvm_gc_pause_seconds`               | JVM MXBean              | Длительность GC-пауз. Длительные паузы замедляют обработку batch job                                |
| `http_server_requests_seconds`       | Micrometer              | Время ответа HTTP-эндпоинтов Actuator. Для мониторинга здоровья приложения                          |
| `spring_batch_job`                   | Spring Batch Micrometer | Статус и время выполнения Batch Job. Ключевая бизнес-метрика                                        |

### Обоснование выбора

Метрики разделены на три уровня:

1. Инфраструктурные (`up`, `process_cpu_usage`, `jvm_memory`) - отвечают на вопрос "работает ли приложение и хватает ли ему ресурсов". Это первое, на что смотрим при инциденте.

2. JVM (`jvm_gc_pause`, `jvm_threads_live`) - отвечают на вопрос "здорова ли JVM". При обработке больших CSV-файлов GC-паузы и thread count - ключевые индикаторы проблем.

3. Бизнес (`spring_batch_job`) - отвечают на вопрос "выполнился ли ETL-процесс успешно". То что волнует бизнес: загрузились ли данные, сколько строк обработано, сколько времени заняла обработка.

## Способ отправки метрик: Prometheus Scraping через Actuator

### Выбранное решение

Spring Boot Actuator + Micrometer Registry Prometheus -> эндпоинт `/actuator/prometheus` -> Prometheus scrape.

### Почему не JMX Exporter

| Критерий             |            Actuator + Micrometer            |                      JMX Exporter                      |
| -------------------- | :-----------------------------------------: | :----------------------------------------------------: |
| Настройка            |        Добавить зависимость, готово         | Нужен Java Agent, конфигурация yaml с маппингом MBeans |
| Кастомные метрики    | Просто: `@Timed`, `MeterRegistry.counter()` |      Сложно: нужно регистрировать MBeans вручную       |
| Spring Batch метрики |       Автоматически через Micrometer        |              Требует кастомного маппинга               |
| Формат данных        |    Нативный Prometheus exposition format    |            Преобразование JMX -> Prometheus             |
| Overhead             |                 Минимальный                 |            Дополнительный Java Agent в JVM             |

Actuator + Micrometer - стандарт Spring Boot экосистемы. Нет причин использовать JMX Exporter, когда нативная интеграция доступна из коробки.

## Способ отправки логов: Filebeat -> Logstash -> Elasticsearch

### Выбранное решение

Приложение пишет JSON-логи в stdout (Logback + logstash-logback-encoder) -> Docker собирает их в файлы -> Filebeat читает Docker-логи -> Logstash парсит и маршрутизирует -> Elasticsearch индексирует -> Kibana отображает.

### Почему JSON-логи через stdout

1. 12-factor app - логи в stdout, без привязки к файловой системе контейнера. Контейнер stateless.
2. Structured logging - JSON-формат позволяет Elasticsearch индексировать поля (`level`, `msg`, `logger`, `trace`) и искать по ним. Текстовые логи требуют парсинга regex, что ненадежно.
3. Trace correlation - `logstash-logback-encoder` автоматически добавляет `traceId` и `spanId` из Micrometer Tracing, позволяя связать логи с распределенными трейсами.

### Почему Filebeat, а не прямая отправка в Logstash

1. Легковесность - Filebeat потребляет минимум ресурсов (Go, ~30 MB RAM). Logstash Appender в Java добавил бы зависимость и сетевые соединения внутри JVM.
2. Буферизация - Filebeat буферизует логи и не теряет их при временной недоступности Logstash/Elasticsearch.
3. Decoupling - приложение не знает о системе логирования. Можно заменить ELK на Loki без изменения кода.

## Конфигурация оповещений

### Почему Prometheus Alert Rules, а не Grafana Alerts

Для данного POC оповещения настроены через Prometheus `alert-rules.yml`:

1. Независимость от UI - алерты работают даже если Grafana недоступна.
2. GitOps - правила хранятся в файле, версионируются в Git вместе с инфраструктурой.
3. Стандарт - Prometheus Alertmanager интегрируется с PagerDuty, Slack, Email, Telegram.

В продакшене следует добавить Alertmanager для маршрутизации алертов в Slack/Email.
