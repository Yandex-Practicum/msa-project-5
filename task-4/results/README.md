# ETL с использованием Spring Batch

## Как развернуть и проверить локально

1. Собрать jar и образ:
   ```bash
   cd batch-processing-app
   ./gradlew build -x test
   docker compose build app
   ```

2. Поднять Postgres:
   ```bash
   docker compose up -d postgresdb
   ```

3. Создать таблицы и загрузить данные программы лояльности (приложение читает `loyality_data` из БД, само туда ничего не загружает — по условию задания эта таблица уже должна существовать):
   ```bash
   docker cp src/main/resources/schema-all.sql batch-processing-app-postgresdb-1:/schema-all.sql
   docker exec batch-processing-app-postgresdb-1 psql -U postgres -d productsdb -f /schema-all.sql
   docker cp src/main/resources/loyality_data.csv batch-processing-app-postgresdb-1:/loyality_data.csv
   docker exec batch-processing-app-postgresdb-1 psql -U postgres -d productsdb -c "\copy loyality_data(productSku, loyalityData) FROM '/loyality_data.csv' WITH (FORMAT csv)"
   ```

4. Запустить сам Job:
   ```bash
   docker compose up app
   ```
   Ожидается `app-1 exited with code 0`, в логе — построчные трансформации и `!!! JOB FINISHED!` с содержимым таблицы `products`.

5. Проверить результат в БД:
   ```bash
   docker exec batch-processing-app-postgresdb-1 psql -U postgres -d productsdb -c "SELECT * FROM products ORDER BY productid;"
   ```
