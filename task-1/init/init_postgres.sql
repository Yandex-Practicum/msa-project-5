CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER,
    amount NUMERIC
);

INSERT INTO orders (user_id, amount) VALUES (1, 100.5), (2, 200.0);