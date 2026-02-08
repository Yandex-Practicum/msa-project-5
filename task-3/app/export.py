import psycopg2
import csv
import os

DB_HOST = os.getenv("DB_HOST", "postgres")
DB_NAME = os.getenv("DB_NAME", "db")
DB_USER = os.getenv("DB_USER", "user")
DB_PASS = os.getenv("DB_PASS", "pass")


def main():
    conn = psycopg2.connect(
        host=DB_HOST,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASS
    )

    cur = conn.cursor()
    cur.execute("SELECT * FROM shipments LIMIT 100")

    rows = cur.fetchall()

    with open("shipments.csv", "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerows(rows)

    print("Export finished")


if __name__ == "__main__":
    main()
