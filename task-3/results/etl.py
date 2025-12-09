import os
import sys
import logging
import psycopg2
from psycopg2.extras import RealDictCursor

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def get_db_connection():
    try:
        db_conn = psycopg2.connect(
            host=os.getenv("DB_HOST", "localhost"),
            port=os.getenv("DB_PORT", "5432"),
            database=os.getenv("DB_NAME", "postgres"),
            user=os.getenv("DB_USER", "postgres"),
            password=os.getenv("DB_PASSWORD", "postgres")
        )
        return db_conn
    except Exception as e:
        logger.exception(f"Unable to connect to a database: {e}")

        raise e

def export_users():
    db_conn = get_db_connection()
    cur = db_conn.cursor(cursor_factory=RealDictCursor)
    
    try:
        logger.info("Begin users export")
        table_name = 'users'
        
        query = f"SELECT * FROM {table_name}"
        cur.execute(query)
        rows = cur.fetchall()
        row_count = len(rows)
        
        logger.info(rows)

        logger.info(f"Users export successfully finished. Fetched {row_count} rows from table '{table_name}'.")

    except Exception as e:
        logger.exception(f"Users export failed: {e}")
        raise e
    finally:
        cur.close()
        db_conn.close()

if __name__ == "__main__":
    export_users()
