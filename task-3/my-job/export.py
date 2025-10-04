import psycopg2
import csv
import os

def export_table():
    db_host = os.getenv('DB_HOST', 'localhost')
    db_name = os.getenv('DB_NAME', 'testdb')
    db_user = os.getenv('DB_USER', 'dbuser')
    db_password = os.getenv('DB_PASSWORD', 'your_password')

    try:
        conn = psycopg2.connect(
            host=db_host,
            database=db_name,
            user=db_user,
            password=db_password
        )
        cur = conn.cursor()

        cur.execute("SELECT * FROM shipments;")
        rows = cur.fetchall()
        columns = [desc[0] for desc in cur.description]

        print("Headers shipments:")
        print(columns)

        print("Data shipments:")
        for row in rows:
            print(row)

        with open('/export/shipments_export.csv', 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(columns)
            writer.writerows(rows)

        print(f"Экспорт таблицы shipments завершён. Файл: /export/shipments_export.csv, строк: {len(rows)}")

    except Exception as e:
        print(f"Ошибка при экспорте: {e}")
    finally:
        if conn:
            cur.close()
            conn.close()

if __name__ == "__main__":
    export_table()