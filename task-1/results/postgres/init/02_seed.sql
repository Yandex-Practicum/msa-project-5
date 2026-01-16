INSERT INTO users (user_id, email, city) VALUES
  (501, 'u501@example.local', 'Helsinki'),
  (502, 'u502@example.local', 'Espoo'),
  (503, 'u503@example.local', 'Vantaa')
ON CONFLICT DO NOTHING;

INSERT INTO orders (order_id, user_id, created_at, total_amount) VALUES
  (1001, 501, '2026-01-15T09:00:00Z', 120.50),
  (1002, 502, '2026-01-15T11:15:00Z',  75.00),
  (1003, 503, '2026-01-15T13:05:00Z', 220.10),
  (1004, 501, '2026-01-15T16:40:00Z',  15.99)
ON CONFLICT DO NOTHING;

INSERT INTO payments (order_id, status, amount, paid_at) VALUES
  (1001, 'PAID', 120.50, '2026-01-15T09:05:00Z'),
  (1002, 'PAID',  75.00, '2026-01-15T11:20:00Z'),
  (1003, 'FAILED',220.10, NULL),
  (1004, 'PAID',  15.99, '2026-01-15T16:45:00Z');
