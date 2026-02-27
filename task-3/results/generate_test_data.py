#!/usr/bin/env python3
"""
Генератор тестовых данных для логистической системы
Объёмы: 
  - drivers: 3,000
  - vehicles: 1,500
  - clients: 30,000
  - shipments: 150,000
  - shipment_events: 750,000 (5 событий на перевозку)
"""
import os
import sys
import logging
from datetime import datetime, timedelta
import random
import psycopg2
from psycopg2.extras import execute_values
from faker import Faker
from tqdm import tqdm

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

# Параметры генерации (настраиваемые)
CONFIG = {
    "drivers_count": 3000,
    "vehicles_count": 1500,
    "clients_count": 30000,
    "shipments_count": 150000,
    "events_per_shipment": 5,  # Среднее количество событий на перевозку
    "batch_size": 5000,        # Размер пачки для вставки
    "start_date": datetime(2023, 1, 1),
    "end_date": datetime(2024, 12, 31)
}

# Инициализация генератора данных
fake = Faker('ru_RU')
Faker.seed(42)
random.seed(42)

# Списки для связей
driver_ids = []
vehicle_ids = []
client_ids = []
shipment_ids = []

def get_db_connection():
    """Подключение к БД из переменных окружения"""
    try:
        return psycopg2.connect(
            host=os.getenv("DB_HOST", "localhost"),
            port=int(os.getenv("DB_PORT", "5432")),
            database=os.getenv("DB_NAME", "logistics"),
            user=os.getenv("DB_USER", "postgres"),
            password=os.getenv("DB_PASSWORD", "postgres")
        )
    except Exception as e:
        logger.error(f"Ошибка подключения к БД: {e}")
        sys.exit(1)

def create_tables(conn):
    """Создание таблиц (если не существуют)"""
    with conn.cursor() as cur:
        cur.execute("""
        DROP TABLE drivers CASCADE;
        CREATE TABLE IF NOT EXISTS drivers (
            id SERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            license_number VARCHAR(20) NOT NULL,
            phone VARCHAR(20),
            rating DECIMAL(3,2) DEFAULT 4.5,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        DROP TABLE vehicles CASCADE;
        CREATE TABLE IF NOT EXISTS vehicles (
            id SERIAL PRIMARY KEY,
            plate_number VARCHAR(15) UNIQUE NOT NULL,
            model VARCHAR(100) NOT NULL,
            capacity_tons DECIMAL(5,2) NOT NULL,
            type VARCHAR(50) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        DROP TABLE clients CASCADE;
        CREATE TABLE IF NOT EXISTS clients (
            id SERIAL PRIMARY KEY,
            name VARCHAR(150) NOT NULL,
            email VARCHAR(100) UNIQUE NOT NULL,
            phone VARCHAR(20),
            address TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        DROP TABLE shipments CASCADE;
        CREATE TABLE IF NOT EXISTS shipments (
            id SERIAL PRIMARY KEY,
            client_id INTEGER NOT NULL REFERENCES clients(id),
            driver_id INTEGER NOT NULL REFERENCES drivers(id),
            vehicle_id INTEGER NOT NULL REFERENCES vehicles(id),
            origin VARCHAR(200) NOT NULL,
            destination VARCHAR(200) NOT NULL,
            status VARCHAR(20) NOT NULL,
            weight_kg DECIMAL(10,2) NOT NULL,
            distance_km INTEGER NOT NULL,
            cost DECIMAL(10,2) NOT NULL,
            scheduled_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        DROP TABLE shipment_events CASCADE;
        CREATE TABLE IF NOT EXISTS shipment_events (
            id SERIAL PRIMARY KEY,
            shipment_id INTEGER NOT NULL REFERENCES shipments(id),
            event_type VARCHAR(30) NOT NULL,
            event_time TIMESTAMP NOT NULL,
            location VARCHAR(200),
            description TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        
        -- Индексы для ускорения запросов
        CREATE INDEX IF NOT EXISTS idx_shipments_scheduled ON shipments(scheduled_at);
        CREATE INDEX IF NOT EXISTS idx_shipments_client ON shipments(client_id);
        CREATE INDEX IF NOT EXISTS idx_events_shipment ON shipment_events(shipment_id);
        CREATE INDEX IF NOT EXISTS idx_events_time ON shipment_events(event_time);
        """)
    conn.commit()
    logger.info("✅ Таблицы созданы или уже существуют")

def generate_drivers(conn):
    """Генерация водителей"""
    global driver_ids
    logger.info(f"Генерация {CONFIG['drivers_count']} водителей...")
    
    drivers = []
    license_prefixes = [
        "177", "178", "150", "199", "197", "192",
        "277", "278", "250", "299", "297", "292",
        "377", "378", "350", "399", "397", "392",
        "477", "478", "450", "499", "497", "492",
        "577", "578", "550", "599", "597", "592",
        "677", "678", "650", "699", "697", "692",
        "777", "778", "750", "799", "797", "792",
        "877", "888", "850", "899", "897", "892",
        "977", "998", "950", "999", "997", "992",
        "77", "78", "50", "99", "97", "92"]
    
    for _ in tqdm.tqdm(range(CONFIG['drivers_count']), desc="Водители"):
        license_num = f"{random.choice(license_prefixes)} № {fake.license_plate()}"
        drivers.append((
            fake.name(),
            license_num,
            fake.phone_number(),
            round(random.uniform(3.5, 5.0), 2)
        ))
    
    # Вставка пачками
    with conn.cursor() as cur:
        for i in range(0, len(drivers), CONFIG['batch_size']):
            batch = drivers[i:i + CONFIG['batch_size']]
            execute_values(
                cur,
                "INSERT INTO drivers (name, license_number, phone, rating) VALUES %s RETURNING id",
                batch
            )
            driver_ids.extend([row[0] for row in cur.fetchall()])
    conn.commit()
    logger.info(f"✅ Сгенерировано {len(driver_ids)} водителей")

def generate_vehicles(conn):
    """Генерация транспорта"""
    global vehicle_ids
    logger.info(f"Генерация {CONFIG['vehicles_count']} единиц транспорта...")
    
    vehicles = []
    models = [
        ("КамАЗ-5490", 20.0, "грузовик"),
        ("МАЗ-5440", 18.5, "грузовик"),
        ("Урал NEXT", 15.0, "грузовик"),
        ("ГАЗель NEXT", 1.5, "фургон"),
        ("Рено Магнум", 22.0, "седельный тягач"),
        ("Скания R500", 24.0, "седельный тягач"),
        ("Мерседес Актрос", 25.0, "седельный тягач"),
        ("Фольксваген Крафтер", 3.5, "фургон")
    ]
    
    for _ in tqdm.tqdm(range(CONFIG['vehicles_count']), desc="Транспорт"):
        model, capacity, v_type = random.choice(models)
        vehicles.append((
            f"{fake.license_plate()} RUS",
            model,
            capacity,
            v_type
        ))
    
    with conn.cursor() as cur:
        for i in range(0, len(vehicles), CONFIG['batch_size']):
            batch = vehicles[i:i + CONFIG['batch_size']]
            execute_values(
                cur,
                "INSERT INTO vehicles (plate_number, model, capacity_tons, type) VALUES %s RETURNING id",
                batch
            )
            vehicle_ids.extend([row[0] for row in cur.fetchall()])
    conn.commit()
    logger.info(f"✅ Сгенерировано {len(vehicle_ids)} единиц транспорта")

def generate_clients(conn):
    """Генерация клиентов"""
    global client_ids
    logger.info(f"Генерация {CONFIG['clients_count']} клиентов...")
    email_prefixes = list("abcdefghijklmnopqrstuvwxyz1234567890")
    clients = []
    for _ in tqdm.tqdm(range(CONFIG['clients_count']), desc="Клиенты"):
        clients.append((
            fake.company(),
            f"{random.choice(email_prefixes)}{random.choice(email_prefixes)}{random.choice(email_prefixes)}-{fake.email()}",
            fake.phone_number(),
            fake.address().replace('\n', ', ')
        ))
    
    with conn.cursor() as cur:
        for i in range(0, len(clients), CONFIG['batch_size']):
            batch = clients[i:i + CONFIG['batch_size']]
            execute_values(
                cur,
                "INSERT INTO clients (name, email, phone, address) VALUES %s RETURNING id",
                batch
            )
            client_ids.extend([row[0] for row in cur.fetchall()])
    conn.commit()
    logger.info(f"✅ Сгенерировано {len(client_ids)} клиентов")

def generate_shipments(conn):
    """Генерация перевозок"""
    global shipment_ids
    logger.info(f"Генерация {CONFIG['shipments_count']} перевозок...")
    
    shipments = []
    statuses = ["scheduled", "in_transit", "delivered", "delayed", "cancelled"]
    cities = [
        "Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань",
        "Нижний Новгород", "Челябинск", "Самара", "Омск", "Ростов-на-Дону",
        "Уфа", "Красноярск", "Воронеж", "Пермь", "Волгоград"
    ]
    
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM clients")
        rows = cur.fetchall()
        id_list = [row[0] for row in rows]
        client_ids.extend(id_list)

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM drivers")
        rows = cur.fetchall()
        id_list = [row[0] for row in rows]
        driver_ids.extend(id_list)

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM vehicles")
        rows = cur.fetchall()
        id_list = [row[0] for row in rows]
        vehicle_ids.extend(id_list)

    for _ in tqdm.tqdm(range(CONFIG['shipments_count']), desc="Перевозки"):
        # Случайные даты в диапазоне
        scheduled = fake.date_time_between(
            start_date=CONFIG['start_date'],
            end_date=CONFIG['end_date']
        )
        
        # Случайные города (разные)
        origin, destination = random.sample(cities, 2)
        
        # Расчёт параметров перевозки
        distance = random.randint(100, 3000)
        weight = round(random.uniform(500, 15000), 2)
        base_cost = distance * 15 + weight * 0.8
        cost = round(base_cost * random.uniform(0.9, 1.2), 2)
        
        shipments.append((
            random.choice(client_ids),
            random.choice(driver_ids),
            random.choice(vehicle_ids),
            origin,
            destination,
            random.choice(statuses),
            weight,
            distance,
            cost,
            scheduled
        ))
    
    with conn.cursor() as cur:
        for i in range(0, len(shipments), CONFIG['batch_size']):
            batch = shipments[i:i + CONFIG['batch_size']]
            execute_values(
                cur,
                """INSERT INTO shipments (
                    client_id, driver_id, vehicle_id, origin, destination, 
                    status, weight_kg, distance_km, cost, scheduled_at
                ) VALUES %s RETURNING id""",
                batch
            )
            shipment_ids.extend([row[0] for row in cur.fetchall()])
    conn.commit()
    logger.info(f"✅ Сгенерировано {len(shipment_ids)} перевозок")

def generate_shipment_events(conn):
    """Генерация событий перевозок"""
    logger.info(f"Генерация ~{CONFIG['shipments_count'] * CONFIG['events_per_shipment']} событий перевозок...")
    
    events = []
    event_types = [
        ("loaded", "Груз погружен"),
        ("departed", "Отправление из пункта А"),
        ("checkpoint", "Промежуточная точка"),
        ("delayed", "Задержка в пути"),
        ("arrived", "Прибытие в пункт Б"),
        ("delivered", "Груз доставлен"),
        ("incident", "Инцидент в пути")
    ]
    
    # Города для событий (расширенный список)
    event_locations = [
        "Склад отправителя", "Погрузочная площадка", "КПП", "АЗС", "Станция техобслуживания",
        "Промежуточный склад", "Таможня", "Пункт разгрузки", "Склад получателя"
    ]
    
    total_events = CONFIG['shipments_count'] * CONFIG['events_per_shipment']
    with tqdm.tqdm(total=total_events, desc="События") as pbar:
        for shipment_id in shipment_ids:
            # Случайное количество событий (3-7)
            num_events = random.randint(3, 7)
            base_time = fake.date_time_between(
                start_date=CONFIG['start_date'],
                end_date=CONFIG['end_date']
            )
            
            for i in range(num_events):
                event_type, desc = random.choice(event_types)
                # Время события сдвинуто относительно базового
                event_time = base_time + timedelta(hours=random.randint(1, 24) * i)
                
                events.append((
                    shipment_id,
                    event_type,
                    event_time,
                    random.choice(event_locations),
                    desc
                ))
                
                # Пакетная вставка при достижении размера пачки
                if len(events) >= CONFIG['batch_size']:
                    _insert_events_batch(conn, events)
                    pbar.update(len(events))
                    events = []
        
        # Вставка остатка
        if events:
            _insert_events_batch(conn, events)
            pbar.update(len(events))
    
    logger.info(f"✅ Сгенерировано {total_events} событий перевозок")

def _insert_events_batch(conn, events):
    """Вспомогательная функция для пакетной вставки событий"""
    with conn.cursor() as cur:
        execute_values(
            cur,
            """INSERT INTO shipment_events (
                shipment_id, event_type, event_time, location, description
            ) VALUES %s""",
            events
        )
    conn.commit()

def print_summary(conn):
    """Вывод статистики по БД"""
    with conn.cursor() as cur:
        cur.execute("""
            SELECT 
                (SELECT COUNT(*) FROM drivers) as drivers,
                (SELECT COUNT(*) FROM vehicles) as vehicles,
                (SELECT COUNT(*) FROM clients) as clients,
                (SELECT COUNT(*) FROM shipments) as shipments,
                (SELECT COUNT(*) FROM shipment_events) as events,
                (SELECT MIN(scheduled_at) FROM shipments) as first_shipment,
                (SELECT MAX(scheduled_at) FROM shipments) as last_shipment
        """)
        stats = cur.fetchone()
    
    print("\n" + "="*50)
    print("📊 СТАТИСТИКА БАЗЫ ДАННЫХ")
    print("="*50)
    print(f"Водители (drivers):       {stats[0]:,}")
    print(f"Транспорт (vehicles):     {stats[1]:,}")
    print(f"Клиенты (clients):        {stats[2]:,}")
    print(f"Перевозки (shipments):    {stats[3]:,}")
    print(f"События (shipment_events):{stats[4]:,}")
    print(f"Период данных:            {stats[5].date()} — {stats[6].date()}")
    print("="*50 + "\n")

def main():
    logger.info("🚀 Запуск генератора тестовых данных для логистической системы")
    
    # Проверка переменных окружения
    required_vars = ["DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD"]
    missing = [v for v in required_vars if not os.getenv(v)]
    if missing:
        logger.warning(f"⚠️  Отсутствуют переменные окружения: {', '.join(missing)}")
        logger.info("Используются значения по умолчанию (локальная БД)")
    
    # Подключение и генерация
    conn = get_db_connection()
    try:
        create_tables(conn)
        generate_drivers(conn)
        generate_vehicles(conn)
        generate_clients(conn)
        generate_shipments(conn)
        generate_shipment_events(conn)
        print_summary(conn)
        logger.info("🎉 Генерация данных завершена успешно!")
    except Exception as e:
        logger.error(f"❌ Критическая ошибка: {e}", exc_info=True)
        sys.exit(1)
    finally:
        conn.close()

if __name__ == "__main__":
    # Установка зависимостей (если запускается впервые)
    try:
        import faker
        import tqdm
    except ImportError:
        print("Установка зависимостей...")
        os.system("pip install faker tqdm psycopg2-binary")
        import faker
        import tqdm
    
    main()