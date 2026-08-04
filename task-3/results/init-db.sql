-- Схема БД платформы грузоперевозок и тестовые данные

-- Заказчики (~10 000-50 000 строк в продуктиве)
CREATE TABLE IF NOT EXISTS clients (
    client_id     BIGSERIAL PRIMARY KEY,
    company_name  VARCHAR(200) NOT NULL,
    contact_name  VARCHAR(150),
    contact_phone VARCHAR(32),
    contact_email VARCHAR(200),
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Водители (~1 000-5 000 строк)
CREATE TABLE IF NOT EXISTS drivers (
    driver_id   BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    license_no  VARCHAR(40) NOT NULL UNIQUE,
    phone       VARCHAR(32),
    hired_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Транспорт (~500-2 000 строк)
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id   BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(16) NOT NULL UNIQUE,
    model        VARCHAR(100) NOT NULL,
    max_load_kg  INTEGER NOT NULL,
    added_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Перевозки (~50 000-200 000 строк)
CREATE TABLE IF NOT EXISTS shipments (
    shipment_id      BIGSERIAL PRIMARY KEY,
    track_no         VARCHAR(32) NOT NULL UNIQUE,
    origin_city      VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    weight_kg        NUMERIC(10,2) NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'created',
    client_id        BIGINT NOT NULL REFERENCES clients(client_id),
    driver_id        BIGINT REFERENCES drivers(driver_id),
    vehicle_id       BIGINT REFERENCES vehicles(vehicle_id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- События по перевозкам (~500 000-1 000 000 строк)
CREATE TABLE IF NOT EXISTS shipment_events (
    event_id    BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipments(shipment_id),
    event_type  VARCHAR(40) NOT NULL,
    city        VARCHAR(100),
    details     TEXT,
    happened_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_shipments_created_at ON shipments(created_at);
CREATE INDEX IF NOT EXISTS ix_events_shipment ON shipment_events(shipment_id);
CREATE INDEX IF NOT EXISTS ix_events_happened_at ON shipment_events(happened_at);

-- Тестовые данные: справочники
INSERT INTO clients (company_name, contact_name, contact_phone, contact_email) VALUES
('АО "СеверГруз"', 'Ольга Тарасова', '+7-921-111-2233', 'tarasova@severgruz.ru'),
('ООО "Волга-Транс"', 'Игорь Белов', '+7-917-222-3344', 'belov@volgatrans.ru'),
('ИП Кравцов А.Н.', 'Андрей Кравцов', '+7-903-333-4455', 'kravtsov@mail.ru'),
('ООО "Дальний Маршрут"', 'Светлана Орлова', '+7-914-444-5566', 'orlova@dalroute.ru'),
('АО "УралКарго"', 'Павел Лебедев', '+7-922-555-6677', 'lebedev@uralcargo.ru'),
('ООО "Столичные Грузы"', 'Наталья Фролова', '+7-916-666-7788', 'frolova@stolgruz.ru');

INSERT INTO drivers (full_name, license_no, phone) VALUES
('Геннадий Савельев', '77СН102938', '+7-905-101-2020'),
('Руслан Хайруллин', '16МР475839', '+7-917-202-3030'),
('Виктор Шаповалов', '23КТ556677', '+7-918-303-4040'),
('Тимур Гарипов', '02АБ889900', '+7-927-404-5050'),
('Станислав Ершов', '66УЮ112233', '+7-912-505-6060'),
('Олег Зимин', '50МК445566', '+7-903-606-7070');

INSERT INTO vehicles (plate_number, model, max_load_kg) VALUES
('К123МР77', 'Volvo FH16', 20000),
('А456ВЕ16', 'КамАЗ-5490', 18000),
('М789ОР23', 'MAN TGX', 19000),
('Т012УК02', 'МАЗ-5440', 16000),
('Е345ХН66', 'Scania R500', 21000),
('О678СТ50', 'ГАЗель Next', 1500);

-- Тестовые данные: перевозки за сегодня и прошлые дни
INSERT INTO shipments (track_no, origin_city, destination_city, weight_kg, status, client_id, driver_id, vehicle_id, created_at) VALUES
-- Сегодня
('SH-100001', 'Москва', 'Нижний Новгород', 12500.00, 'in_transit', 1, 1, 1, now() - INTERVAL '6 hours'),
('SH-100002', 'Казань', 'Уфа', 8400.50, 'in_transit', 2, 2, 2, now() - INTERVAL '5 hours'),
('SH-100003', 'Самара', 'Волгоград', 15200.00, 'delivered', 3, 3, 3, now() - INTERVAL '4 hours'),
('SH-100004', 'Екатеринбург', 'Челябинск', 6300.75, 'in_transit', 4, 4, 4, now() - INTERVAL '3 hours'),
('SH-100005', 'Москва', 'Тверь', 1100.00, 'delivered', 5, 6, 6, now() - INTERVAL '2 hours'),
('SH-100006', 'Тула', 'Рязань', 9800.00, 'created', 6, 5, 5, now() - INTERVAL '1 hour'),
-- Вчера
('SH-099901', 'Москва', 'Владимир', 7000.00, 'delivered', 1, 1, 1, now() - INTERVAL '1 day'),
('SH-099902', 'Казань', 'Ижевск', 5400.00, 'delivered', 2, 2, 2, now() - INTERVAL '1 day' - INTERVAL '1 hour'),
-- Позавчера
('SH-099801', 'Самара', 'Саратов', 13200.00, 'delivered', 3, 3, 3, now() - INTERVAL '2 days');

-- Тестовые данные: события (за сегодня попадут в выгрузку)
INSERT INTO shipment_events (shipment_id, event_type, city, details, happened_at) VALUES
(1, 'accepted', 'Москва', 'Заявка принята в работу', now() - INTERVAL '6 hours'),
(1, 'loaded', 'Москва', 'Погрузка завершена, пломба 004512', now() - INTERVAL '5 hours 30 minutes'),
(1, 'departed', 'Москва', 'ТС выехало со склада', now() - INTERVAL '5 hours'),
(2, 'accepted', 'Казань', 'Заявка принята в работу', now() - INTERVAL '5 hours'),
(2, 'loaded', 'Казань', 'Погрузка завершена', now() - INTERVAL '4 hours 20 minutes'),
(3, 'accepted', 'Самара', 'Заявка принята в работу', now() - INTERVAL '4 hours'),
(3, 'departed', 'Самара', 'ТС выехало со склада', now() - INTERVAL '3 hours 40 minutes'),
(3, 'arrived', 'Волгоград', 'ТС прибыло на склад получателя', now() - INTERVAL '2 hours'),
(3, 'delivered', 'Волгоград', 'Груз передан получателю', now() - INTERVAL '90 minutes'),
(4, 'accepted', 'Екатеринбург', 'Заявка принята в работу', now() - INTERVAL '3 hours'),
(5, 'accepted', 'Москва', 'Заявка принята в работу', now() - INTERVAL '2 hours'),
(5, 'delivered', 'Тверь', 'Груз передан получателю', now() - INTERVAL '30 minutes'),
(6, 'created', 'Тула', 'Заявка создана', now() - INTERVAL '1 hour'),
-- Вчерашние события (в выгрузку не попадут)
(7, 'accepted', 'Москва', 'Заявка принята в работу', now() - INTERVAL '1 day'),
(8, 'delivered', 'Ижевск', 'Груз передан получателю', now() - INTERVAL '1 day' + INTERVAL '5 hours');
