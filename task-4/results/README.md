# Task 4: ETL с использованием Spring Batch

## Содержимое

- [ADR.md](ADR.md) — обоснование выбора Spring Batch с анализом альтернатив
- [c4_spring_batch.puml](c4_spring_batch.puml) — C4-диаграмма архитектуры системы
- Spring Batch приложение — доработанный шаблон с реализацией ETL

## Что реализовано

ETL-приложение на Spring Batch:
1. Extract: чтение товаров из `product-data.csv` (`FlatFileItemReader`)
2. Transform: обогащение данными из таблицы `loyality_data` (`ProductItemProcessor`)
3. Load: запись обработанных товаров в таблицу `products` (`JdbcBatchItemWriter`)

Таблицы и справочные данные создаются автоматически при старте приложения через `schema-all.sql` и `data.sql`.

## Запуск

```bash
cd task-4/results
docker compose up --build
```

## Ожидаемый результат

В логах приложения:

```
Transforming (Product[productId=1, productSku=20001, productName=hammer, productAmount=45, productData=Loyality_off])
  into (Product[productId=1, productSku=20001, productName=hammer, productAmount=45, productData=Loyality_on])
...
!!! JOB FINISHED! Time to verify the results
Transformed <Product[productId=1, productSku=20001, productName=hammer, productAmount=45, productData=Loyality_on]> in the database.
```

Товары с SKU 20001, 30001, 50001, 60001 получают `productData=Loyality_on` из таблицы `loyality_data`. Товар с SKU 40001 сохраняет исходное значение, так как его нет в таблице лояльности.

## Проверка данных в БД

```bash
psql -h localhost -p 5432 -U postgres -d productsdb -c "SELECT * FROM products;"
# Пароль: 123456
```

## Перезапуск с чистой БД

```bash
docker compose down -v
docker compose up --build
```
