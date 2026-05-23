#!/bin/bash
set -euo pipefail

NAMESPACE="data-export"

echo ">>> Применяем манифесты..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/cronjob.yaml

echo ">>> Создаём тестовый ручной запуск..."
JOB_NAME="export-manual-$(date +%s)"

kubectl create job --from=cronjob/daily-export "${JOB_NAME}" \
    -n "${NAMESPACE}" \
    --dry-run=client -o yaml | kubectl apply -f -

echo ">>> Ждём завершения (макс 120 секунд)..."
kubectl wait --for=condition=complete "job/${JOB_NAME}" \
    -n "${NAMESPACE}" --timeout=120s 2>/dev/null || true

echo ">>> Логи:"
kubectl logs "job/${JOB_NAME}" -n "${NAMESPACE}" || true

echo ">>> Статус:"
kubectl get job "${JOB_NAME}" -n "${NAMESPACE}"

echo ">>> CronJob в кластере:"
kubectl get cronjob -n "${NAMESPACE}"