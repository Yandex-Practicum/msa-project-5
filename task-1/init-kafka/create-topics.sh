#!/bin/bash
set -e

echo "Starting Kafka"

/etc/confluent/docker/run &

echo "Waiting for Kafka ready..."
until kafka-topics.sh --bootstrap-server kafka:29092 --list > /dev/null 2>&1; do
  echo "Kafka not ready yet, waiting"
  sleep 5
done

echo "Kafka is ready! Creating topic 'order_events'"
kafka-topics.sh --create \
  --topic order_events \
  --bootstrap-server kafka:29092 \
  --partitions 1 \
  --replication-factor 1 || true

echo "Topic 'order_events' created"

wait