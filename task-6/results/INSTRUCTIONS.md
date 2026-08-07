# Запуск и проверка Task 6

## 1. Запуск

Из корневой директории `msa-project-5` выполните:

```bash
docker compose -f task-6/results/docker-compose.yml down -v
POSTGRES_PORT=55432 APP_PORT=18080 ELASTICSEARCH_PORT=19200 KIBANA_PORT=15601 docker compose -f task-6/results/docker-compose.yml up --build -d
docker compose -f task-6/results/docker-compose.yml ps
```

Дождитесь состояния `healthy` у `postgres`, `app`, `elasticsearch` и `logstash`:

```bash
curl http://localhost:18080/actuator/health
```

До API-вызова Job не должен выполняться:

```bash
docker compose -f task-6/results/docker-compose.yml logs app | rg 'JOB_STARTED|JOB_COMPLETED'
```

Пустой результат подтверждает, что автоматический запуск отключён.

## 2. Запуск Job через Java-клиент

```bash
java task-6/results/client/BatchJobClient.java
```

Клиент выведет `traceId`, HTTP-статус и результат Job. Значение `traceId` в первой строке и JSON-ответе должно совпадать.

Проверьте JSON-логи приложения:

```bash
docker compose -f task-6/results/docker-compose.yml logs app | rg 'HTTP_REQUEST|JOB_STARTED|STEP_COMPLETED|JOB_COMPLETED'
```

У событий должны присутствовать `traceId` и `spanId`. У `HTTP_REQUEST` также должен быть URI `/api/v1/jobs/import-products`.

## 3. Проверка Elasticsearch

```bash
curl 'http://localhost:19200/_cat/indices/batch-processing-*?v'
```

Если индекс ещё не появился, подождите несколько секунд и проверьте доставку:

```bash
docker compose -f task-6/results/docker-compose.yml logs --tail=100 filebeat logstash
```

## 4. Проверка Kibana

1. Откройте <http://localhost:15601>.
2. Откройте **Stack Management → Data Views**. В старой версии пункт называется **Index Patterns**.
3. Создайте Data View с именем `Batch Processing`, шаблоном `batch-processing-*` и полем времени `@timestamp`.
4. Откройте **Analytics → Discover** и выберите `Batch Processing`.
5. Установите период **Last 1 hour**.
6. Введите KQL-запрос:

   ```text
   event : "HTTP_REQUEST" and traceId : * and spanId : * and uri : "/api/v1/jobs/import-products"
   ```

7. Добавьте столбцы `event`, `traceId`, `spanId`, `uri`, `httpMethod`, `httpStatus` и `durationMs`.
8. Для просмотра всей обработки замените KQL-фильтр на `traceId : "<traceId из ответа клиента>"` и нажмите **Refresh**.
9. В поиске полей слева найдите и добавьте `jobName` и `status`. Если поиск ничего не показывает, обновите поля Data View через **Stack Management → Data Views → Batch Processing → Refresh field list**, затем вернитесь в Discover.

В одном trace должны находиться `JOB_STARTED`, `STEP_COMPLETED`, `JOB_COMPLETED` и `HTTP_REQUEST`.

## 5. Остановка

```bash
docker compose -f task-6/results/docker-compose.yml down
```
