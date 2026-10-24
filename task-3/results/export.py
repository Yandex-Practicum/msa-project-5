import csv
import os
from pathlib import Path

import psycopg


def main():
    destination = Path(os.environ.get("EXPORT_DIR", "/exports")) / "shipments.csv"
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(".csv.tmp")
    with psycopg.connect(os.environ["DATABASE_URL"]) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT id, client_id, status FROM shipments ORDER BY id")
            with temporary.open("w", newline="", encoding="utf-8") as stream:
                writer = csv.writer(stream)
                writer.writerow(["id", "client_id", "status"])
                writer.writerows(cursor)
    temporary.replace(destination)
    print(f"Экспорт завершён: {destination}")


if __name__ == "__main__":
    main()
