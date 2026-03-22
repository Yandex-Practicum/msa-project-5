-- Тестовая таблица для шага «чтение из БД» в POC
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    amount INT DEFAULT 1
);
INSERT INTO orders (amount) VALUES (10), (20);
