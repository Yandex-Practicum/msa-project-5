CREATE TABLE IF NOT EXISTS orders (
    order_id TEXT PRIMARY KEY,
    customer_email TEXT NOT NULL,
    customer_segment TEXT NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    order_id TEXT PRIMARY KEY REFERENCES orders(order_id),
    payment_status TEXT NOT NULL
);

TRUNCATE TABLE payments, orders CASCADE;

INSERT INTO orders (order_id, customer_email, customer_segment, total_amount) VALUES
    ('ORD-1001', 'anna@example.com', 'vip', 149.90),
    ('ORD-1002', 'boris@example.com', 'standard', 42.50),
    ('ORD-1003', 'carol@example.com', 'vip', 220.00),
    ('ORD-1004', 'daria@example.com', 'standard', 15.99),
    ('ORD-1005', 'egor@example.com', 'new', 88.40);

INSERT INTO payments (order_id, payment_status) VALUES
    ('ORD-1001', 'paid'),
    ('ORD-1002', 'paid'),
    ('ORD-1003', 'refunded'),
    ('ORD-1004', 'paid'),
    ('ORD-1005', 'paid');
