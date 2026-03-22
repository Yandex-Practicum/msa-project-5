"""
Простой экспорт одной таблицы в CSV (задание 3).
Переменные окружения: PGHOST, PGPORT, PGUSER, PGPASSWORD, PGDATABASE, TABLE_NAME, OUT_DIR
"""
import csv
import os

import psycopg2


ALLOWED_TABLES = frozenset({"shipments", "drivers", "vehicles", "clients", "shipment_events"})


def main() -> None:
    table = os.environ.get("TABLE_NAME", "shipments")
    if table not in ALLOWED_TABLES:
        raise ValueError(f"TABLE_NAME must be one of {sorted(ALLOWED_TABLES)}")
    out_dir = os.environ.get("OUT_DIR", "/out")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, f"{table}.csv")

    conn = psycopg2.connect(
        host=os.environ.get("PGHOST", "localhost"),
        port=os.environ.get("PGPORT", "5432"),
        user=os.environ.get("PGUSER", "demo"),
        password=os.environ.get("PGPASSWORD", "demo"),
        dbname=os.environ.get("PGDATABASE", "demo"),
    )
    try:
        cur = conn.cursor()
        cur.execute(f'SELECT * FROM {table}')
        rows = cur.fetchall()
        colnames = [d[0] for d in cur.description]
    finally:
        conn.close()

    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(colnames)
        w.writerows(rows)

    print(f"Written {len(rows)} rows to {out_path}")


if __name__ == "__main__":
    main()
