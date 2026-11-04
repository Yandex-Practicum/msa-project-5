import os
from pathlib import Path

import psycopg2


def main():
    output = Path("/reports")
    temporary = output / "shipments.csv.tmp"
    result = output / "shipments.csv"

    try:
        with psycopg2.connect(
            host=os.environ["PGHOST"],
            dbname=os.environ["PGDATABASE"],
            user=os.environ["PGUSER"],
            password=os.environ["PGPASSWORD"],
            connect_timeout=10,
        ) as connection:
            with connection.cursor() as cursor:
                with temporary.open("w", encoding="utf-8", newline="") as csv_file:
                    cursor.copy_expert(
                        "COPY (SELECT id, client_name, destination, shipped_at "
                        "FROM shipments ORDER BY id) TO STDOUT WITH (FORMAT CSV, HEADER TRUE)",
                        csv_file,
                    )
        os.replace(temporary, result)
        print(f"Export completed: {result}")
    finally:
        temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
