CREATE TABLE products (
    product_id BIGINT PRIMARY KEY,
    product_sku BIGINT NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    product_amount BIGINT NOT NULL CHECK (product_amount >= 0),
    product_data VARCHAR(120) NOT NULL
);

CREATE TABLE loyality_data (
    product_sku BIGINT PRIMARY KEY,
    loyality_data VARCHAR(120) NOT NULL
);

COPY loyality_data (product_sku, loyality_data)
FROM '/docker-entrypoint-initdb.d/loyality_data.csv'
WITH (FORMAT csv);
