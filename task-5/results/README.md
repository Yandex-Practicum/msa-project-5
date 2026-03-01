# Документ: Обоснование выбора способа отправки логов и конфигурации оповещений

**Проект:** Реализация мониторинга и логирования для Spring Batch ETL  
**Автор:** Степанов Андрей  
**Дата:** 01.03.2026  

---

## 1. Введение

Данный документ содержит обоснование архитектурных решений по организации централизованного сбора логов и системы оповещений для Spring Batch приложения обработки данных о товарах.

### 1.1 Контекст
- Приложение: Spring Boot 3.5 + Spring Batch 5.2
- Инфраструктура: Docker Compose (локально), планируется миграция в Kubernetes
- Объём данных: до 1 млн строк/день в CSV, ~50-200 МБ логов/день
- Требования: наблюдаемость, быстрое обнаружение инцидентов, аудит

### 1.2 Цели документа
1. Обосновать выбор технологии отправки логов в Elasticsearch
2. Обосновать выбор системы алертинга и конфигурацию правил
3. Предоставить рекомендации по эксплуатации и масштабированию

---

## 2. Выбор способа отправки логов

### 2.1 Рассмотренные альтернативы

| Решение | Архитектура | Плюсы | Минусы | Оценка |
|---------|-------------|-------|--------|--------|
| **Filebeat → Logstash → Elasticsearch** | Push-модель, буферизация | ✅ Встроенная обработка backpressure, парсинг JSON, enrichment полей, стабильность | ⚠️ Дополнительный компонент в стеке | ✅ **Выбрано** |
| **Filebeat → Elasticsearch (напрямую)** | Push-модель, минимализм | ✅ Проще архитектура, меньше задержка | ⚠️ Нет сложной обработки/трансформации логов | ⚠️ Альтернатива |
| **Fluentd → Elasticsearch** | Push-модель, гибкость | ✅ Мощный routing, плагины | ⚠️ Сложнее конфигурация, Ruby-скрипты | ❌ |
| **Loki + Promtail** | Pull/Push, cloud-native | ✅ Лёгкий, нативная интеграция с Grafana | ⚠️ Ограниченный full-text search, меньше зрелость | ⚠️ Для future |
| **Прямая запись приложения в Elasticsearch** | Push из кода | ✅ Минимум компонентов | ❌ Tight coupling, риск потери логов при падении БД, нагрузка на app | ❌ |

### 2.2 Обоснование выбора: Filebeat → Logstash → Elasticsearch

#### 2.2.1 Технические преимущества

1. **Надёжность доставки**
   - Filebeat использует ACK-механизм: подтверждение получения перед удалением из буфера
   - Встроенный retry с exponential backoff при недоступности downstream
   - Disk-based queue при переполнении памяти

2. **Обработка и трансформация**
   - Logstash позволяет:
     - Парсить структурированные JSON-логи
     - Добавлять geoip, user-agent parsing
     - Фильтровать чувствительные данные (PII masking)
     - Маршрутизировать логи по уровню в разные индексы

3. **Минимальное влияние на приложение**
   - Приложение пишет в локальный файл — асинхронно, без блокировок
   - Filebeat читает файл с tail-моделью — низкое потребление CPU/RAM
   - Нет сетевого handshake на критическом пути бизнес-логики

4. **Экосистема и поддержка**
   - Официальная интеграция с Elastic Stack
   - Активное сообщество, регулярные обновления безопасности
   - Совместимость с будущим переходом на ECK (Elastic Cloud on Kubernetes)

#### 2.2.2 Конфигурация Filebeat

```yaml
# filebeat.yml
filebeat.inputs:
  - type: docker
    combine_partial: true
    containers:
      path: "/usr/share/dockerlogs/data"
      stream: "stdout"
      ids:
        - "*"
    exclude_files: ['\.gz$']
    ignore_older: 10m

processors:
  # decode the log field (sub JSON document) if JSON encoded, then maps it's fields to elasticsearch fields
  - decode_json_fields:
      fields: ["log", "message", "msg"]
      target: ""
      # overwrite existing target elasticsearch fields while decoding json fields
      overwrite_keys: true
  - add_docker_metadata:
      # annotates each event with relevant metadata from Docker containers
      host: "unix:///var/run/docker.sock"
  - drop_fields:
      # drop the fields below in order to have a cleaner
      fields: ["container.labels.io_buildpacks_build_metadata", "container.labels.io_buildpacks_lifecycle_metadata", "container.labels.io_buildpacks_stack_id"]

filebeat.config.modules:
  path: ${path.config}/modules.d/*.yml
  reload.enabled: false

# setup filebeat to send output to logstash
output.logstash:
  hosts: ["logstash"]

# Write Filebeat own logs only to file to avoid catching them with itself in docker log files
logging.level: info
logging.to_files: false
logging.to_syslog: false
loggins.metrice.enabled: false
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0644
ssl.verification_mode: none
```

#### 2.2.3 Конфигурация Logstash

```ruby
input {
	beats {
	    port => 5044
	    host => "0.0.0.0"
  	}
}

output {
	elasticsearch {
		hosts => elasticsearch
		manage_template => false
    	index => "%{[@metadata][beat]}-%{[@metadata][version]}-%{+YYYY.MM.dd}"
	}

    stdout { codec => rubydebug }
}
```

---

## 3. Выбор системы оповещений

### 3.1 Рассмотренные альтернативы

| Решение | Плюсы | Минусы | Оценка |
|---------|-------|--------|--------|
| **Prometheus Alertmanager** | ✅ Cloud-native, декларативные правила, интеграция с Prometheus, rich routing | ⚠️ Отдельный компонент, требуется настройка receivers | ✅ **Выбрано** |
| **Grafana Alerting** | ✅ Единый UI с дашбордами, визуальный редактор правил | ⚠️ Меньше гибкости в routing, привязка к Grafana | ⚠️ Альтернатива |
| **Elastic Watcher** | ✅ Алёрты на основе логов, full-text search условия | ❌ Платная функция X-Pack, vendor lock-in | ❌ |
| **Custom webhook + скрипты** | ✅ Полный контроль | ❌ Высокие операционные затраты, сложно поддерживать | ❌ |

### 3.2 Обоснование выбора: Prometheus Alertmanager

#### 3.2.1 Технические преимущества

1. **Декларативные правила**
   - Правила хранятся в YAML, версионируются в Git
   - Легко тестировать через `promtool check rules`
   - Поддержка templating для динамических аннотаций

2. **Гибкий routing**
   - Маршрутизация по severity, team, alertname
   - Grouping для уменьшения alert fatigue
   - Inhibition rules для подавления зависимых алертов

3. **Интеграции**
   - Native поддержка: Email, Slack, PagerDuty, Opsgenie, Webhook
   - Легко добавить custom receiver через webhook

4. **Cloud-native совместимость**
   - Работает в Kubernetes без изменений
   - Поддержка service discovery для динамических target'ов

#### 3.2.2 Конфигурация правил алертов

```yaml
groups:
  - name: batch-processing-alerts
    rules:
      # Приложение упало
      - alert: BatchAppDown
        expr: up{job="batch-processing"} == 0
        for: 1m
        labels:
          severity: critical
          service: batch-processing
        annotations:
          summary: "Приложение batch-processing недоступно"
          description: "Метрика 'up' = 0 более 1 минуты. Проверьте логи контейнера app."

      # Высокая загрузка CPU
      - alert: HighCpuUsage
        expr: process_cpu_usage{job="batch-processing"} > 0.7
        for: 2m
        labels:
          severity: warning
          service: batch-processing
        annotations:
          summary: "Высокая загрузка CPU ({{ $value | humanizePercentage }})"
          description: "Загрузка CPU превышает 70% в течение 2 минут."

      # Высокое использование памяти
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap", job="batch-processing"} / jvm_memory_max_bytes{area="heap", job="batch-processing"}) * 100 > 80
        for: 3m
        labels:
          severity: warning
          service: batch-processing
        annotations:
          summary: "Высокое использование памяти ({{ $value | humanizePercentage }})"
          description: "Использование heap памяти превышает 80%."

      # Ошибки в job'ах
      - alert: BatchJobFailed
        expr: increase(batch_job_failed_total{job="batch-processing"}[5m]) > 0
        for: 1m
        labels:
          severity: critical
          service: batch-processing
        annotations:
          summary: "Batch job завершился с ошибкой"
          description: "Обнаружены ошибки в выполнении batch job за последние 5 минут."

      # Медленная обработка данных
      - alert: SlowProcessing
        expr: rate(batch_items_processed_total{job="batch-processing"}[5m]) < 10
        for: 5m
        labels:
          severity: warning
          service: batch-processing
        annotations:
          summary: "Низкая скорость обработки данных"
          description: "Обрабатывается менее 10 записей в минуту."

      # Нет данных
      - alert: NoDataProcessed
        expr: increase(batch_items_processed_total{job="batch-processing"}[10m]) == 0
        for: 10m
        labels:
          severity: warning
          service: batch-processing
        annotations:
          summary: "Нет обработанных данных"
          description: "За последние 10 минут не было обработано ни одной записи."

      # Долгое выполнение GC
      - alert: LongGCPause
        expr: rate(jvm_gc_pause_seconds_sum{job="batch-processing"}[5m]) > 1
        for: 2m
        labels:
          severity: warning
          service: batch-processing
        annotations:
          summary: "Длительная пауза GC"
          description: "Время GC превышает 1 секунду."

      # Много открытых файлов
      - alert: HighFileDescriptors
        expr: jvm_fd_usage{job="batch-processing"} > 0.8
        for: 5m
        labels:
          severity: warning
          service: batch-processing
        annotations:
          summary: "Много открытых файловых дескрипторов"
          description: "Использовано более 80% доступных файловых дескрипторов."
```

#### 3.2.3 Конфигурация Alertmanager

```yaml
global:
  resolve_timeout: 5m
  # Настройки для Telegram
  telegram_api_url: "https://api.telegram.org"

route:
  group_by: ['alertname', 'service', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 4h
  receiver: 'telegram-notifications'
  routes:
    - match:
        severity: critical
      receiver: 'telegram-critical'
      continue: true
    - match:
        severity: warning
      receiver: 'telegram-warnings'
      continue: true

receivers:
  - name: 'telegram-notifications'
    telegram_configs:
      - bot_token: 'YOUR_BOT_TOKEN'
        chat_id: YOUR_CHAT_ID
        api_url: "https://api.telegram.org"
        parse_mode: 'HTML'
        message: |-
          <b>{{ .Status | toUpper }}</b>
          <b>Alert:</b> {{ .GroupLabels.alertname }}
          <b>Service:</b> {{ .GroupLabels.service }}
          <b>Severity:</b> {{ .GroupLabels.severity }}
          {{ range .Alerts }}
            {{ if .Annotations.summary }}
          <b>Summary:</b> {{ .Annotations.summary }}
            {{ end }}
            {{ if .Annotations.description }}
          <b>Description:</b> {{ .Annotations.description }}
            {{ end }}
          <b>Value:</b> {{ .Annotations.value }}
          <b>Started:</b> {{ .StartsAt.Format "2006-01-02 15:04:05" }}
          {{ end }}

  - name: 'telegram-critical'
    telegram_configs:
      - bot_token: 'YOUR_BOT_TOKEN'
        chat_id: YOUR_CHAT_ID
        api_url: "https://api.telegram.org"
        parse_mode: 'HTML'
        message: |-
          🚨 <b>КРИТИЧЕСКИЙ АЛЕРТ</b> 🚨
          {{ range .Alerts }}
          <b>Alert:</b> {{ .Labels.alertname }}
          <b>Summary:</b> {{ .Annotations.summary }}
          <b>Description:</b> {{ .Annotations.description }}
          {{ end }}

  - name: 'telegram-warnings'
    telegram_configs:
      - bot_token: 'YOUR_BOT_TOKEN'
        chat_id: YOUR_CHAT_ID
        api_url: "https://api.telegram.org"
        parse_mode: 'HTML'
        message: |-
          ⚠️ <b>ПРЕДУПРЕЖДЕНИЕ</b> ⚠️
          {{ range .Alerts }}
          <b>Alert:</b> {{ .Labels.alertname }}
          <b>Summary:</b> {{ .Annotations.summary }}
          {{ end }}


```

---

## 4. Операционные соображения

### 4.1 Управление шумом алертов

| Проблема | Решение |
|----------|---------|
| Alert fatigue | Grouping + repeat_interval: 4h, inhibition rules |
| Ложные срабатывания | for: 1-2m, использование percentiles вместо instant values |
| Пропущенные инциденты | Multi-condition alerts, escalation policies |
| Ночные алерты | Time-based routing, on-call rotation |

### 4.2 Безопасность

1. **Секреты**
   - Хранить в Kubernetes Secrets / HashiCorp Vault
   - Не коммитить в Git: использовать `.env` + `.gitignore`

2. **Сетевая изоляция**
   - Filebeat → Logstash: internal network only
   - Alertmanager web UI: behind auth proxy

3. **Аудит**
   - Логировать изменения в правилах алертов
   - Включить audit log в Elasticsearch

---

## 5. Метрики

| № | Название алерта | Метрика/Expression | Порог | Длительность | Severity | Описание | Действия |
|---|-----------------|-------------------|-------|--------------|----------|----------|----------|
| **Доступность приложения** |
| 1 | `BatchAppDown` | `up{job="batch-processing"}` | `== 0` | 1 мин | 🔴 Critical | Приложение недоступно | 1. Проверить статус контейнера<br>2. Проверить логи: `docker-compose logs app`<br>3. Проверить health endpoint |
| **Производительность** |
| 2 | `HighCpuUsage` | `process_cpu_usage{job="batch-processing"}` | `> 0.7` (70%) | 2 мин | 🟡 Warning | Высокая загрузка CPU | 1. Проверить активные джобы<br>2. Проверить метрики GC<br>3. Рассмотреть увеличение лимитов CPU |
| 3 | `HighMemoryUsage` | `(jvm_memory_used_bytes / jvm_memory_max_bytes) * 100` | `> 80%` | 3 мин | 🟡 Warning | Высокое использование heap памяти | 1. Проверить heap dump<br>2. Увеличить `-Xmx` при необходимости<br>3. Оптимизировать размер чанка |
| 4 | `LongGCPause` | `rate(jvm_gc_pause_seconds_sum[5m])` | `> 1 сек` | 2 мин | 🟡 Warning | Длительная пауза GC | 1. Проверить метрики GC<br>2. Включить GC logging<br>3. Рассмотреть G1GC или ZGC |
| 5 | `HighFileDescriptors` | `jvm_fd_usage{job="batch-processing"}` | `> 0.8` (80%) | 5 мин | 🟡 Warning | Много открытых файловых дескрипторов | 1. Проверить ulimit<br>2. Проверить утечки файловых потоков<br>3. Проверить ротацию логов |
| **Batch Job Execution** |
| 6 | `BatchJobFailed` | `increase(batch_job_failed_total[5m])` | `> 0` | 1 мин | 🔴 Critical | Job завершился с ошибкой | 1. Проверить логи на FlatFileParseException<br>2. Проверить доступность CSV<br>3. Проверить подключение к БД |
| 7 | `SlowProcessing` | `rate(batch_items_processed_total[5m])` | `< 10` записей/мин | 5 мин | 🟡 Warning | Низкая скорость обработки | 1. Проверить длительность step<br>2. Проверить lock'и в БД<br>3. Оптимизировать запросы |
| 8 | `NoDataProcessed` | `increase(batch_items_processed_total[10m])` | `== 0` | 10 мин | 🟡 Warning | Нет обработанных данных | 1. Проверить skip_count<br>2. Проверить логи writer'а<br>3. Проверить конфигурацию Step |

---

## 6. Roadmap улучшений

### Краткосрочно (1-3 месяца)
- [ ] Интеграция с корпоративным SSO для Grafana/Kibana
- [ ] Автоматическое создание dashboards через Terraform
- [ ] Синтетические тесты для proactive alerting

### Среднесрочно (3-6 месяцев)
- [ ] ML-based anomaly detection для метрик длительности
- [ ] Интеграция с incident management (PagerDuty/Opsgenie)
- [ ] Distributed tracing (Jaeger/Tempo) для end-to-end observability

### Долгосрочно (6+ месяцев)
- [ ] Переход на Kubernetes + ECK для production
- [ ] Multi-cluster aggregation через Thanos/Cortex
- [ ] Cost optimization через log sampling для debug-уровня

---

## 7. Заключение

Выбранный стек **Filebeat → Logstash → Elasticsearch** для логирования и **Prometheus Alertmanager** для оповещений обеспечивает:

✅ **Надёжность**: гарантированная доставка логов, retry-механизмы, буферизация  
✅ **Гибкость**: декларативная конфигурация, богатые возможности routing и трансформации  
✅ **Масштабируемость**: cloud-native архитектура, готовность к Kubernetes  
✅ **Безопасность**: разделение секретов, сетевая изоляция, аудит  
✅ **Эксплуатация**: минимальный operational overhead, богатая экосистема инструментов  

Данное решение полностью соответствует текущим требованиям проекта и обеспечивает плавный путь миграции в облачную инфраструктуру компании.
