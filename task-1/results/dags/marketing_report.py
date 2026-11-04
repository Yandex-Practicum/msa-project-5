import csv
import json
import logging
import os
from datetime import datetime, timedelta, timezone
from pathlib import Path

from airflow import DAG
from airflow.operators.python import PythonOperator

DATA = Path("/opt/airflow/data")
OUTPUT = Path("/opt/airflow/output")
TOPIC = "order-events"


def publish_events(**context):
    from kafka import KafkaProducer

    producer = KafkaProducer(
        bootstrap_servers="kafka:9092",
        value_serializer=lambda value: json.dumps(value).encode("utf-8"),
        retries=5,
    )
    try:
        for order_id, status, timestamp in [
            (101, "created", "2025-01-01T10:00:00Z"),
            (101, "confirmed", "2025-01-01T11:00:00Z"),
            (102, "created", "2025-01-01T12:00:00Z"),
            (103, "created", "2025-01-01T13:00:00Z"),
        ]:
            producer.send(TOPIC, {
                "run_id": context["run_id"], "order_id": order_id,
                "status": status, "event_time": timestamp,
            }).get(timeout=30)
    finally:
        producer.close()


def make_report(**context):
    import psycopg2
    from kafka import KafkaConsumer, TopicPartition

    with psycopg2.connect(
        host="postgres", dbname="airflow", user="airflow", password="airflow"
    ) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT order_id, user_id, amount FROM orders ORDER BY order_id")
            orders = cursor.fetchall()

    with (DATA / "deliveries.csv").open(encoding="utf-8", newline="") as file:
        deliveries = {int(row["order_id"]): row for row in csv.DictReader(file)}
    with (DATA / "payments.json").open(encoding="utf-8") as file:
        payments = {row["order_id"]: row for row in json.load(file)}
    with (DATA / "users.json").open(encoding="utf-8") as file:
        users = {row["user_id"]: row for row in json.load(file)}

    consumer = KafkaConsumer(
        bootstrap_servers="kafka:9092", enable_auto_commit=False,
        value_deserializer=lambda value: json.loads(value.decode("utf-8")),
    )
    events = {}
    try:
        partitions = consumer.partitions_for_topic(TOPIC)
        if not partitions:
            raise RuntimeError("Kafka topic has no available partitions")
        assigned = [TopicPartition(TOPIC, partition) for partition in partitions]
        consumer.assign(assigned)
        consumer.seek_to_beginning(*assigned)
        ends = consumer.end_offsets(assigned)
        while any(consumer.position(partition) < ends[partition] for partition in assigned):
            records = consumer.poll(timeout_ms=10000)
            if not records:
                raise RuntimeError("Timed out while reading Kafka events")
            for partition, messages in records.items():
                for message in messages:
                    if message.offset >= ends[partition]:
                        continue
                    event = message.value
                    if event.get("run_id") != context["run_id"]:
                        continue
                    previous = events.get(event["order_id"])
                    if previous is None or event["event_time"] > previous["event_time"]:
                        events[event["order_id"]] = event
    finally:
        consumer.close()

    OUTPUT.mkdir(parents=True, exist_ok=True)
    temporary = OUTPUT / "marketing_report.csv.tmp"
    fields = ["order_id", "user_id", "user_name", "segment", "amount",
              "payment_status", "delivery_status", "last_event", "event_time"]
    with temporary.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields)
        writer.writeheader()
        for order_id, user_id, amount in orders:
            user = users.get(user_id, {})
            event = events.get(order_id, {})
            writer.writerow({
                "order_id": order_id, "user_id": user_id,
                "user_name": user.get("user_name", ""), "segment": user.get("segment", ""),
                "amount": amount, "payment_status": payments.get(order_id, {}).get("payment_status", ""),
                "delivery_status": deliveries.get(order_id, {}).get("delivery_status", ""),
                "last_event": event.get("status", ""), "event_time": event.get("event_time", ""),
            })
    os.replace(temporary, OUTPUT / "marketing_report.csv")
    logging.info("Report written: %s orders", len(orders))


with DAG(
    dag_id="marketing_report",
    start_date=datetime(2025, 1, 1, tzinfo=timezone.utc),
    schedule=None,
    catchup=False,
    max_active_runs=1,
    default_args={
        "retries": 1,
        "retry_delay": timedelta(seconds=30),
        "email": ["alerts@example.com"],
        "email_on_failure": True,
        "email_on_retry": False,
    },
    tags=["marketing", "demo"],
) as dag:
    publish = PythonOperator(task_id="publish_demo_events", python_callable=publish_events)
    report = PythonOperator(task_id="build_report", python_callable=make_report)
    publish >> report
