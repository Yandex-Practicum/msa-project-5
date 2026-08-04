-- Инициализация тестовых данных маркетингового контура.
-- БД airflow (метаданные) создаётся переменными окружения POSTGRES_*.
-- Здесь создаём отдельную БД marketing как «источник данных» для пайплайна.

CREATE DATABASE marketing;

\c marketing

-- Расширенные данные о пользователях
CREATE TABLE users (
    user_id     TEXT PRIMARY KEY,
    full_name   TEXT NOT NULL,
    segment     TEXT NOT NULL,
    city        TEXT NOT NULL
);

-- Заказы
CREATE TABLE orders (
    order_id         TEXT PRIMARY KEY,
    user_id          TEXT NOT NULL REFERENCES users(user_id),
    product_category TEXT NOT NULL,
    amount           NUMERIC(10, 2) NOT NULL,
    currency         TEXT NOT NULL DEFAULT 'RUB',
    created_at       DATE NOT NULL
);

-- Платежи по заказам
CREATE TABLE payments (
    payment_id     TEXT PRIMARY KEY,
    order_id       TEXT NOT NULL REFERENCES orders(order_id),
    payment_status TEXT NOT NULL,        -- paid / failed / pending / refunded
    paid_at        TIMESTAMP
);

-- 30 пользователей: сегменты и города детерминированы от номера
INSERT INTO users (user_id, full_name, segment, city)
SELECT
    'USER-' || lpad(g::text, 4, '0'),
    'Клиент ' || g,
    (ARRAY['standard', 'premium', 'b2b'])[1 + (g % 3)],
    (ARRAY['Москва', 'Санкт-Петербург', 'Казань', 'Новосибирск', 'Екатеринбург'])[1 + (g % 5)]
FROM generate_series(1, 30) AS g;

-- 30 заказов ORD-1001..ORD-1030
INSERT INTO orders (order_id, user_id, product_category, amount, currency, created_at)
SELECT
    'ORD-' || (1000 + g),
    'USER-' || lpad(g::text, 4, '0'),
    (ARRAY['electronics', 'clothing', 'books', 'grocery'])[1 + (g % 4)],
    round((50 + (g * 37 % 450))::numeric, 2),
    'RUB',
    DATE '2026-07-01' + (g % 30)
FROM generate_series(1, 30) AS g;

-- Платежи: заказы g = 14 и g = 28 (ORD-1014, ORD-1028) — неуспешные
INSERT INTO payments (payment_id, order_id, payment_status, paid_at)
SELECT
    'PAY-' || lpad(g::text, 4, '0'),
    'ORD-' || (1000 + g),
    CASE WHEN g % 14 = 0 THEN 'failed' ELSE 'paid' END,
    CASE WHEN g % 14 = 0 THEN NULL
         ELSE (DATE '2026-07-01' + (g % 30))::timestamp + interval '3 hours'
    END
FROM generate_series(1, 30) AS g;
