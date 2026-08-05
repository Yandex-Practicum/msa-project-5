-- В исходном шаблоне PRIMARY KEY стоял и на productId, и на productSku —
-- PostgreSQL не создаёт таблицу с двумя первичными ключами. Оставлен один, на productId.
CREATE TABLE products  (
    productId BIGINT NOT NULL PRIMARY KEY,
    productSku BIGINT NOT NULL,
    productName VARCHAR(20),
    productAmount BIGINT,
    productData VARCHAR(120)
);

CREATE TABLE loyality_data  (
    productSku BIGINT NOT NULL PRIMARY KEY,
    loyalityData VARCHAR(120)
);