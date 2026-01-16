import os
import sys
import time
import datetime as dt
import psycopg2


def env(name: str, default: str | None = None, required: bool = False) -> str:
    v = os.getenv(name, default)
    if required and (v is None or v == ""):
        raise RuntimeError(f"Missing required env var: {name}")
    return v


def build_conn_params():
    dsn = os.getenv("DATABASE_URL")
    if dsn:
        return {"dsn": dsn}

    return {
        "host": env("DB_HOST", required=True),
        "port": int(env("DB_PORT", "5432")),
        "dbname": env("DB_NAME", required=True),
        "user": env("DB_USER", required=True),
        "password": env("DB_PASSWORD", required=True),
    }


def connect_with_retries(max_attempts: int = 10, base_sleep: float = 2.0):
    params = build_conn_params()
    last_err = None
    for attempt in range(1, max_attempts + 1):
        try:
            if "dsn" in params:
                conn = psycopg2.connect(params["dsn"])
            else:
                conn = psycopg2.connect(**params)
            conn.autocommit = True
            return conn
        except Exception as e:
            last_err = e
            sleep_s = base_sleep * attempt
            print(f"[WARN] DB connect failed attempt={attempt}/{max_attempts}: {e}. Sleep {sleep_s:.1f}s", flush=True)
            time.sleep(sleep_s)
    raise RuntimeError(f"DB connect failed after {max_attempts} attempts: {last_err}")


def main():
    table = env("TABLE", "shipment_events")
    output_dir = env("OUTPUT_DIR", "/output")
    export_date = env("EXPORT_DATE", "")
    date_column = env("DATE_COLUMN", "")

    if not export_date:
        export_date = dt.datetime.utcnow().date().isoformat()

    os.makedirs(output_dir, exist_ok=True)
    out_path = os.path.join(output_dir, f"{table}_{export_date}.csv")

    where_clause = ""
    if date_column:
        where_clause = f" WHERE {date_column}::date = DATE '{export_date}'"

    sql = f"SELECT * FROM {table}{where_clause}"

    copy_cmd = f"COPY ({sql}) TO STDOUT WITH (FORMAT CSV, HEADER TRUE)"

    print(f"[INFO] Export start table={table} export_date={export_date} out={out_path}", flush=True)

    conn = connect_with_retries()
    try:
        with conn.cursor() as cur, open(out_path, "w", encoding="utf-8") as f:
            cur.copy_expert(copy_cmd, f)
        print(f"[INFO] Export completed OK: {out_path}", flush=True)

        try:
            with open(out_path, "r", encoding="utf-8") as f:
                head = []
                for _ in range(5):
                    line = f.readline()
                    if not line:
                        break
                    head.append(line.rstrip("\n"))
            print("[INFO] CSV head:\n" + "\n".join(head), flush=True)
        except Exception as e:
            print(f"[WARN] Cannot print head: {e}", flush=True)

    finally:
        conn.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"[ERROR] {e}", file=sys.stderr, flush=True)
        sys.exit(1)
