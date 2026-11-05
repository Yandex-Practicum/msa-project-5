# Задание 5: наблюдаемость пакетной обработки

Приложение задания 4 дополнено HTTP-запуском, метриками и логами. Состав решения и обоснование — в [`../results`](../results/README.md).

## Запуск

Нужны Docker Compose, доступ к Docker Hub, Elastic и Maven, свободные порты 3000, 5433, 5601, 8080, 9090, 9093 и 9200; ELK требует несколько гигабайт памяти. Команды выполняются из `task-5/solution`:

```bash
docker compose up -d --build
docker compose ps
```

Первый запуск может занять несколько минут: необходимо скачать образы и дождаться Elasticsearch и Kibana. PostgreSQL создаёт таблицы и заполняет справочник только при создании нового тома. Приложение остаётся запущенным; задание запускается отдельно:

```bash
curl -X POST http://localhost:8080/jobs/import
```

Ожидаемый ответ: `executionId=..., status=COMPLETED`. Каждый запрос создаёт новый экземпляр задания; запись товаров по `productId` идемпотентна. Это локальный синхронный HTTP API без авторизации, не предназначенный для публикации в интернет.

```bash
docker compose exec postgresdb psql -U postgres -d productsdb -c 'SELECT * FROM products ORDER BY productId;'
curl -s http://localhost:8080/actuator/prometheus | grep tradeware_batch
```

Ожидаются пять товаров, счётчики успешно выполненного задания и пяти прочитанных и записанных строк. Обновление Prometheus и дашборда может занять несколько циклов опроса.

## Метрики и дашборд

- Grafana: <http://localhost:3000>, вход `admin` / `admin`, папка **TradeWare**, дашборд **TradeWare: пакетная обработка**.
- Prometheus: <http://localhost:9090>; запрос `up{job="batch-processing"}`. Приложение опрашивает отдельный Prometheus Agent; в интерфейсе Targets основного Prometheus агентский target не отображается, его метрики поступают через remote write.
- Alertmanager: <http://localhost:9093>. Локальный получатель выводит доставленные webhook-уведомления в `docker compose logs alert-receiver`.

Проверка оповещения о недоступности:

```bash
docker compose stop app
```

Через 30 секунд и несколько циклов опроса и доставки проверьте Alertmanager, страницу Alerts в Prometheus и `docker compose logs alert-receiver`. Верните приложение командой `docker compose start app`. Это демонстрационный срок; не используйте его как производственный порог.

## Логи в Kibana

После запуска задания дождитесь доставки и убедитесь, что индекс создан:

```bash
curl 'http://localhost:9200/_cat/indices/tradeware-batch-*?v'
```

Откройте <http://localhost:5601>, перейдите в **Stack Management → Index Patterns**, создайте шаблон `tradeware-batch-*` с полем времени `@timestamp`. В **Discover** выберите его и подходящий временной интервал. Записи содержат начало и завершение задания и `executionId`. Если индекс пока не создан, проверьте `docker compose logs filebeat logstash`.

## Данные и безопасность

Имя Compose-проекта и имена Docker-томов заданы явно и не зависят от имени каталога. `docker compose down` останавливает окружение и сохраняет данные; `docker compose down -v` удаляет его тома и данные. Пароли, незашифрованный доступ, открытые порты и webhook-приёмник предназначены только для локальной демонстрации. Агент Prometheus и Filebeat запускаются от root в контейнерах; для промышленного окружения нужны отдельные пользователи с минимальными правами на тома. Логи ротации хранятся 7 дней, но политика очистки индексов Elasticsearch и защита доступа не настроены.
