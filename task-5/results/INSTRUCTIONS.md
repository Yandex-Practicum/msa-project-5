# Запуск и проверка Task 5

## 1. Запуск

Из корневой директории проекта `msa-project-5` выполните:

```bash
docker compose -f task-5/results/docker-compose.yml down -v
POSTGRES_PORT=55432 APP_PORT=18080 docker compose -f task-5/results/docker-compose.yml up --build -d
docker compose -f task-5/results/docker-compose.yml ps
```

Используются внешние порты `55432` и `18080`, потому что стандартные `5432` и `8080` могут быть заняты другими учебными проектами. Внутри Docker Compose используются обычные `5432` и `8080`.

Дождитесь запуска всех сервисов. Spring Batch должен завершить Job, а контейнер `app` — остаться в состоянии `Up`:

```bash
docker compose -f task-5/results/docker-compose.yml logs app | rg 'JOB_COMPLETED|JOB_FAILED'
curl http://localhost:18080/actuator/health
curl http://localhost:18080/actuator/prometheus | rg batch_job
```

## 2. Grafana

1. Откройте <http://localhost:3000> и войдите как `admin` / `admin`.
2. Откройте dashboard **Batch Processing**.
3. Откройте **Alerting → Alert rules**.
4. Убедитесь, что существуют `BatchJobFailed` и `BatchJobSlow`.

Prometheus доступен по адресу <http://localhost:9090>.

## 3. Kibana

### 3.1. Проверка данных

Убедитесь, что Filebeat создал индекс:

```bash
curl 'http://localhost:9200/_cat/indices/batch-processing-*?v'
```

В результате должна быть строка с индексом вида `batch-processing-YYYY.MM.dd`. Если её нет, проверьте доставку логов:

```bash
docker compose -f task-5/results/docker-compose.yml logs --tail=100 filebeat logstash
```

### 3.2. Создание Data View

1. Откройте <http://localhost:5601>.
2. Если появилась стартовая страница, выберите **Explore on my own**.
3. Откройте меню слева и выберите **Stack Management**.
4. В разделе **Kibana** откройте **Data Views**. В некоторых версиях пункт называется **Index Patterns**.
5. Нажмите **Create data view**.
6. В поле **Name** укажите `Batch Processing`.
7. В поле **Index pattern** укажите `batch-processing-*`.
8. В поле **Timestamp field** выберите `@timestamp`.
9. Нажмите **Save data view to Kibana**.

### 3.3. Просмотр batch-событий

1. Откройте меню **Analytics → Discover**.
2. Выберите Data View **Batch Processing**.
3. Справа сверху установите период **Last 1 hour** и нажмите **Refresh**.
4. В строке KQL введите:

   ```text
   service : "batch-processing" and event : *
   ```

5. Добавьте в таблицу поля:
   - `event`;
   - `jobName`;
   - `status`;
   - `readCount`;
   - `writeCount`;
   - `skipCount`;
   - `durationMs`.
6. Убедитесь, что присутствуют `JOB_STARTED`, `STEP_COMPLETED` и `JOB_COMPLETED`.
7. Для `STEP_COMPLETED` проверьте значения `readCount=5`, `writeCount=5`, `skipCount=0`.

Если таблица пустая, увеличьте период до **Last 24 hours**, нажмите **Refresh** и ещё раз проверьте индекс командой из раздела 3.1.

## 4. Остановка

```bash
docker compose -f task-5/results/docker-compose.yml down
```

Команда `docker compose down -v` дополнительно удаляет локальные данные этого демонстрационного окружения.
