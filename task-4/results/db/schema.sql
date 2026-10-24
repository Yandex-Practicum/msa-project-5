CREATE TABLE products (
    product_id BIGINT PRIMARY KEY,
    product_sku BIGINT NOT NULL,
    product_name VARCHAR(100),
    product_amount BIGINT,
    product_data VARCHAR(120)
);

CREATE TABLE loyality_data (
    product_sku BIGINT PRIMARY KEY,
    loyalty_data VARCHAR(120)
);

COPY loyality_data (product_sku, loyalty_data) FROM '/docker-entrypoint-initdb.d/loyality_data.csv' WITH (FORMAT csv);
