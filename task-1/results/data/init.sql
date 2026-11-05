CREATE TABLE orders (
    order_id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    amount NUMERIC(10, 2) NOT NULL
);

INSERT INTO orders (order_id, user_id, amount) VALUES
    (101, 1, 1200.00),
    (102, 2, 800.00),
    (103, 1, 500.00);
