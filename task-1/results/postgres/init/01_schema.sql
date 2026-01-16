CREATE TABLE IF NOT EXISTS users (
  user_id     BIGINT PRIMARY KEY,
  email       TEXT NOT NULL,
  city        TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
  order_id      BIGINT PRIMARY KEY,
  user_id       BIGINT NOT NULL REFERENCES users(user_id),
  created_at    TIMESTAMPTZ NOT NULL,
  total_amount  NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
  payment_id  BIGSERIAL PRIMARY KEY,
  order_id    BIGINT NOT NULL REFERENCES orders(order_id),
  status      TEXT NOT NULL,
  amount      NUMERIC(12,2) NOT NULL,
  paid_at     TIMESTAMPTZ
);
