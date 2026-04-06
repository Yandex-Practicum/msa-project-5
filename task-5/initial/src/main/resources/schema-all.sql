CREATE TABLE IF NOT EXISTS products (
    productId BIGINT PRIMARY KEY,
    productSku BIGINT NOT NULL UNIQUE,
    productName VARCHAR(64) NOT NULL,
    productAmount BIGINT NOT NULL,
    productData VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS loyality_data (
    productSku BIGINT PRIMARY KEY,
    loyalityData VARCHAR(120) NOT NULL
);
