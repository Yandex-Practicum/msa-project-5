# price_generator.py
import psycopg2, csv, os, logging
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def main():
    # Подключение к БД из переменных окружения
    conn = psycopg2.connect(
        host=os.getenv("DB_HOST"),
        database=os.getenv("DB_NAME"),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD")
    )
    
    # Формирование данных
    cursor = conn.cursor()
    cursor.execute("""
        SELECT c.id, c.name, p.name, cat.name, cp.price
        FROM client_prices cp
        JOIN clients c ON cp.client_id = c.id
        JOIN products p ON cp.product_id = p.id
        JOIN categories cat ON p.category_id = cat.id
        WHERE cp.active = true
    """)
    
    # Группировка по клиентам и запись в CSV
    clients_data = {}
    for row in cursor.fetchall():
        clients_data.setdefault(row[0], []).append(row)
    
    output_dir = "/output"
    for client_id, rows in clients_data.items():
        filename = f"{client_id}_{datetime.now().strftime('%Y%m%d')}.csv"
        with open(os.path.join(output_dir, filename), 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(["Client", "Product", "Category", "Price"])
            for row in rows:
                writer.writerow([row[1], row[2], row[3], row[4]])
    
    logger.info(f"✅ Сгенерировано {len(clients_data)} файлов")
    conn.close()

if __name__ == "__main__":
    main()