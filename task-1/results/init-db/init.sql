-- Инициализация БД 

-- Таблица справочных данных о товарах
CREATE TABLE IF NOT EXISTS product_reference (
    sku VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    unit VARCHAR(20) DEFAULT 'шт',
    price NUMERIC(12, 2),
    promo_discount NUMERIC(5, 2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Таблица остатков на складах
CREATE TABLE IF NOT EXISTS warehouse_inventory (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    warehouse_code VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    product_name VARCHAR(255),
    category VARCHAR(100),
    price NUMERIC(12, 2),
    promo_discount NUMERIC(5, 2) DEFAULT 0,
    report_date DATE NOT NULL,
    loaded_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (sku, warehouse_code, report_date)
);

-- Таблица логов загрузок
CREATE TABLE IF NOT EXISTS load_log (
    id SERIAL PRIMARY KEY,
    filename VARCHAR(500) NOT NULL,
    rows_total INTEGER,
    rows_loaded INTEGER,
    rows_errors INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, PARTIAL
    started_at TIMESTAMP,
    finished_at TIMESTAMP DEFAULT NOW(),
    error_message TEXT
);

-- Наполнение справочных данных
INSERT INTO product_reference (sku, product_name, category, unit, price, promo_discount) VALUES
    ('CEM-001', 'Цемент', 'Цемент и смеси', 'мешок', 450.00, 0),
    ('BRK-001', 'Кирпич 1', 'Кирпич', 'шт', 12.50, 5),
    ('BRK-002', 'Кирпич 2', 'Кирпич', 'шт', 15.00, 0),
    ('PIP-001', 'Труба 1', 'Трубы', 'м.п.', 85.00, 10),
    ('PIP-002', 'Труба 2', 'Трубы', 'м.п.', 42.00, 0),
    ('INS-001', 'Утеплитель 1', 'Изоляция', 'м2', 320.00, 15),
    ('INS-002', 'Утеплитель 2', 'Изоляция', 'м2', 280.00, 0),
    ('WOD-001', 'Доска', 'Пиломатериалы', 'м3', 12000.00, 0),
    ('WOD-002', 'Брус', 'Пиломатериалы', 'м3', 14000.00, 5),
    ('MET-001', 'Арматура', 'Металлопрокат', 'т', 52000.00, 0),
    ('SND-001', 'Песок', 'Сыпучие', 'т', 800.00, 0),
    ('GRV-001', 'Щебень', 'Сыпучие', 'т', 1200.00, 0)
ON CONFLICT (sku) DO NOTHING;
