CREATE TABLE IF NOT EXISTS products  (
    productId BIGINT NOT NULL PRIMARY KEY,
    productSku BIGINT NOT NULL,
    productName VARCHAR(20),
    productAmount BIGINT,
    productData VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS loyality_data  (
    productSku BIGINT NOT NULL PRIMARY KEY,
    loyalityData VARCHAR(120)
);

INSERT INTO loyality_data (productSku, loyalityData) VALUES (20001, 'Loyality_on') ON CONFLICT DO NOTHING;
INSERT INTO loyality_data (productSku, loyalityData) VALUES (30001, 'Loyality_on') ON CONFLICT DO NOTHING;
INSERT INTO loyality_data (productSku, loyalityData) VALUES (50001, 'Loyality_on') ON CONFLICT DO NOTHING;
INSERT INTO loyality_data (productSku, loyalityData) VALUES (60001, 'Loyality_on') ON CONFLICT DO NOTHING;
