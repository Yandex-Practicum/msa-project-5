-- Инициализация БД логистики с тестовыми данными

CREATE TABLE IF NOT EXISTS drivers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    license_number VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    hired_at DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS vehicles (
    id SERIAL PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL,
    model VARCHAR(100),
    capacity_kg INTEGER,
    year INTEGER
);

CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    city VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS shipments (
    id SERIAL PRIMARY KEY,
    client_id INTEGER REFERENCES clients(id),
    driver_id INTEGER REFERENCES drivers(id),
    vehicle_id INTEGER REFERENCES vehicles(id),
    origin_city VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    weight_kg NUMERIC(10, 2),
    status VARCHAR(30) DEFAULT 'created',
    created_at TIMESTAMP DEFAULT NOW(),
    delivered_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shipment_events (
    id SERIAL PRIMARY KEY,
    shipment_id INTEGER REFERENCES shipments(id),
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP DEFAULT NOW(),
    location VARCHAR(200),
    notes TEXT
);

-- Тестовые данные

INSERT INTO drivers (full_name, license_number, phone)
SELECT
    'Водитель ' || i,
    'DL-' || LPAD(i::text, 6, '0'),
    '+7900' || LPAD(i::text, 7, '0')
FROM generate_series(1, 50) AS i;

INSERT INTO vehicles (plate_number, model, capacity_kg, year)
SELECT
    'A' || LPAD(i::text, 3, '0') || 'AA77',
    (ARRAY['КамАЗ 5490', 'МАЗ 5440', 'Volvo FH', 'Scania R450', 'DAF XF'])[1 + i % 5],
    (ARRAY[10000, 15000, 20000, 25000])[1 + i % 4],
    2018 + i % 7
FROM generate_series(1, 30) AS i;

INSERT INTO clients (company_name, contact_email, city)
SELECT
    'Компания ' || i,
    'client' || i || '@example.com',
    (ARRAY['Москва', 'Санкт-Петербург', 'Казань', 'Новосибирск', 'Екатеринбург'])[1 + i % 5]
FROM generate_series(1, 100) AS i;

INSERT INTO shipments (client_id, driver_id, vehicle_id, origin_city, destination_city, weight_kg, status, created_at)
SELECT
    1 + i % 100,
    1 + i % 50,
    1 + i % 30,
    (ARRAY['Москва', 'Санкт-Петербург', 'Казань', 'Новосибирск'])[1 + i % 4],
    (ARRAY['Екатеринбург', 'Самара', 'Ростов-на-Дону', 'Краснодар'])[1 + i % 4],
    500 + (i * 37 % 19500)::numeric,
    (ARRAY['created', 'in_transit', 'delivered', 'cancelled'])[1 + i % 4],
    NOW() - (i || ' minutes')::interval
FROM generate_series(1, 500) AS i;

INSERT INTO shipment_events (shipment_id, event_type, event_time, location)
SELECT
    1 + i % 500,
    (ARRAY['created', 'picked_up', 'in_transit', 'checkpoint', 'delivered'])[1 + i % 5],
    NOW() - (i || ' minutes')::interval,
    (ARRAY['Склад отправления', 'Трасса М7', 'Пункт контроля', 'Склад назначения'])[1 + i % 4]
FROM generate_series(1, 2000) AS i;
