#!/usr/bin/env python3

import csv
import os
from datetime import datetime
from pathlib import Path

import psycopg
from psycopg.sql import SQL, Identifier


def main():
    table_name = os.environ["TABLE_NAME"]

    output_dir = Path(os.environ["OUTPUT_DIR"])

    conn = psycopg.connect(
        host=os.environ["DB_HOST"],
        port=os.environ["DB_PORT"],
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
    )

    with conn.cursor() as cur:
        query = SQL("select * from {}").format(Identifier(table_name))
        cur.execute(query)
        rows = cur.fetchall()

        columns = [desc[0] for desc in cur.description]
        file_name = f"{table_name}-{datetime.now().strftime('%Y%m%d-%H%M%S')}.csv"
        target_file = output_dir / file_name

        with target_file.open("w") as f:
            writer = csv.writer(f)
            writer.writerow(columns)
            writer.writerows(rows)

        print(f"Экспортировано {len(rows)} строк из {table_name} в {target_file}")

    conn.close()


if __name__ == "__main__":
    main()