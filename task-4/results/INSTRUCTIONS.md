# Локальный запуск Task 4

Команды выполняются из корня репозитория.

## 1. Проверить Java-приложение

```bash
./task-4/results/gradlew -p task-4/results clean test
```

## 2. Подготовить окружение

Сначала удалите состояние предыдущего демонстрационного запуска:

```bash
docker compose -f task-4/results/docker-compose.yml \
  down -v --remove-orphans
```

Соберите приложение и дождитесь готовности PostgreSQL:

```bash
docker compose -f task-4/results/docker-compose.yml \
  build app
docker compose -f task-4/results/docker-compose.yml \
  up -d --wait postgres
```

## 3. Запустить ETL и проверить логи

```bash
docker compose -f task-4/results/docker-compose.yml \
  run --rm --no-deps app
```

Ожидаемая итоговая запись:

```text
JOB_COMPLETED job=importProductJob read=5 written=5 skipped=0 products=5
```

## 4. Проверить таблицу products

```bash
docker compose -f task-4/results/docker-compose.yml \
  exec -T postgres \
  psql -U products -d productsdb \
  -c "SELECT product_id, product_sku, product_name, product_amount, product_data FROM products ORDER BY product_id;"
```

Для SKU `20001`, `30001`, `50001` и `60001` значение `product_data` должно стать `Loyality_on`. Для SKU `40001` оно должно остаться `Loyality_off`.

## 5. Подготовить скриншот

Сохраните логи успешного Job и результат SQL-запроса в одном файле:

```text
task-4/results/evidence/task-4-spring-batch-result.png
```

## 6. Остановить окружение

```bash
docker compose -f task-4/results/docker-compose.yml \
  down -v --remove-orphans
```
