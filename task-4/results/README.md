# Task 4 — ETL на Spring Batch (POC)

Решение задания 4 части 2 (кейс TradeWare): пакетная загрузка складского отчёта
`product-data.csv`, обогащение позиций данными программы лояльности из таблицы
`loyality_data` и запись результата в таблицу `products` PostgreSQL.

## Состав

- `ADR-001-spring-batch-etl.md` — архитектурное решение по официальному шаблону (`adr_template.md`): Use Cases, НФТ, обоснование Spring Batch, альтернативы (Airflow, Spark, K8s CronJob), риски.
- `diagrams/c4-context.puml`, `diagrams/c4-container.puml` — C4-диаграммы уровней контекста и контейнеров с вынесенным ETL-компонентом.
- `src/` — Spring Batch-приложение (Java 17, Spring Boot 3.5, Gradle):
  - `BatchConfiguration` — джоба `importProductJob`, чанк-обработка по 100 записей, skip битых строк (до 10) вместо падения всей загрузки;
  - `ProductItemProcessor` — обогащение позиции из справочника `loyality_data` (параметризованный запрос);
  - `JobCompletionNotificationListener` — итоговый лог со статусом, длительностью и содержимым `products`.
- `init-db/01-schema.sql` — создание таблиц `products`, `loyality_data` и наполнение справочника лояльности при первом старте PostgreSQL (соответствует `src/main/resources/schema-all.sql` из шаблона задания).
- `docker-compose.yml`, `Dockerfile`, `.env` — локальное окружение: PostgreSQL 15 + приложение.
- `run-and-screenshots-plan.md` — пошаговая инструкция по запуску с отметками, в какие моменты снимать скриншоты для сдачи.

## Как это работает

1. PostgreSQL при первом старте выполняет `init-db/01-schema.sql`: создаются таблицы и справочник лояльности.
2. Приложение стартует после готовности БД, Spring Batch создаёт свои таблицы `BATCH_*` и запускает `importProductJob`.
3. Reader читает `product-data.csv` из classpath → Processor подставляет `loyalityData` по `productSku` → Writer вставляет чанки по 100 строк в `products` одной транзакцией.
4. По завершении слушатель логирует статус джобы, длительность и все записи `products` — видно, что у SKU из справочника `productData` заменён на `Loyality_on`.

## Быстрый старт

```bash
cd task-4/results
./gradlew build -x test        # собрать jar
docker compose up --build -d   # поднять PostgreSQL + приложение
docker compose logs app        # логи ETL-джобы
```

Подробный сценарий с проверками и точками для скриншотов — в `run-and-screenshots-plan.md`.
