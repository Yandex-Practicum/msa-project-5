#!/bin/bash
# Ручной запуск экспорта без CronJob (для отладки)
set -euo pipefail

docker run --rm \
    -e DB_HOST=host.docker.internal \
    -e DB_PORT=5432 \
    -e DB_NAME=logistics \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres \
    -e EXPORT_TABLE=shipments \
    -e EXPORT_DATE=2025-05-20 \
    -v "$(pwd)/output:/app/output" \
    logistics-export:latest