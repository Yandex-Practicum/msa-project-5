#!/usr/bin/env python3
"""Экспорт данных из PostgreSQL в CSV через psql"""
import os
import sys
import subprocess
import logging
from datetime import datetime, timedelta

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class Exporter:
    def __init__(self):
        self.db_host = os.getenv('DB_HOST', 'postgres')
        self.db_port = os.getenv('DB_PORT', '5432')
        self.db_name = os.getenv('DB_NAME', 'shipments_db')
        self.db_user = os.getenv('DB_USERNAME', 'exporter')
        self.db_password = os.getenv('DB_PASSWORD', 'exporter123')

        self.output_dir = '/tmp/exports'
        os.makedirs(self.output_dir, exist_ok=True)

        self.export_date = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
        self.stats = {}

    def export_table(self, table_name, where_condition=None):
        """Экспорт таблицы через psql"""
        filename = f"{self.output_dir}/{table_name}_{self.export_date}.csv"

        # Строим запрос
        if where_condition:
            query = f"SELECT * FROM {table_name} WHERE {where_condition}"
        else:
            query = f"SELECT * FROM {table_name}"

        # Команда psql
        cmd = [
            'psql',
            '-h', self.db_host,
            '-p', self.db_port,
            '-U', self.db_user,
            '-d', self.db_name,
            '-c', f"\\copy ({query}) TO '{filename}' CSV HEADER"
        ]

        env = os.environ.copy()
        env['PGPASSWORD'] = self.db_password

        logger.info(f"Exporting {table_name}...")
        result = subprocess.run(cmd, env=env, capture_output=True, text=True)

        if result.returncode != 0:
            logger.error(f"Error: {result.stderr}")
            return 0

        # Считаем строки
        try:
            with open(filename, 'r') as f:
                count = sum(1 for _ in f) - 1  # минус заголовок
            self.stats[table_name] = count
            logger.info(f"{table_name}: {count} rows")
            return count
        except:
            logger.error(f"Can't read {filename}")
            return 0

    def run(self):
        logger.info(f"Starting export for date: {self.export_date}")
        start = datetime.now()

        tables = {
            'drivers': None,
            'vehicles': None,
            'clients': None,
            'shipments': f"DATE(created_at) = '{self.export_date}'",
            'shipment_events': f"DATE(created_at) = '{self.export_date}'"
        }

        total = 0
        for table, where in tables.items():
            rows = self.export_table(table, where)
            total += rows

        elapsed = (datetime.now() - start).total_seconds()

        logger.info("=" * 50)
        logger.info(f"✅ Export completed in {elapsed:.2f}s")
        logger.info(f"📊 Total rows: {total}")
        for table, count in self.stats.items():
            logger.info(f"   {table}: {count}")
        logger.info("=" * 50)

        return total > 0

if __name__ == '__main__':
    success = Exporter().run()
    sys.exit(0 if success else 1)