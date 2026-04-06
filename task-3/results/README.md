# Task 3: K8s CronJob для экспорта данных логистики

### 1. Запуск Minikube

```bash
minikube start
```

### 2. Сборка Docker-образа внутри Minikube

```bash
eval $(minikube docker-env)
docker build -t export-table:latest ./docker/
```

### 3. Применение K8s-манифестов

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres.yaml

# Дождаться готовности PostgreSQL
kubectl wait --namespace logistics --for=condition=ready pod -l app=postgres --timeout=120s

kubectl apply -f k8s/export-cronjob.yaml
```

### 4. Ручной запуск Job (не дожидаясь крона)

```bash
kubectl create job --from=cronjob/export-shipments test-export -n logistics
```

### 5. Проверка результата

```bash
# Логи Job
kubectl logs -n logistics job/test-export

# Проверка CSV в PVC
kubectl run --rm -it check-csv -n logistics \
  --image=busybox \
  --overrides='{
    "spec": {
      "containers": [{
        "name": "check",
        "image": "busybox",
        "command": ["sh", "-c", "ls -la /output && head -5 /output/*.csv"],
        "volumeMounts": [{"name": "output", "mountPath": "/output"}]
      }],
      "volumes": [{
        "name": "output",
        "persistentVolumeClaim": {"claimName": "export-output"}
      }],
      "restartPolicy": "Never"
    }
  }' -- sh -c "ls -la /output && echo '---' && head -5 /output/*.csv"
```

### 6. Состояние CronJob

```bash
kubectl get cronjob -n logistics
kubectl get jobs -n logistics
kubectl get pods -n logistics
```

## Конфигурация

Расписание задается в `k8s/export-cronjob.yaml`:

- `schedule: "0 20 * * *"` — каждый день в 20:00
- `backoffLimit: 3` — 3 повторные попытки при ошибке
- `activeDeadlineSeconds: 600` — таймаут 10 минут
- `concurrencyPolicy: Forbid` — не запускать новый, пока предыдущий работает

Таблица для экспорта задается через переменную `TABLE_NAME`. Допустимые значения: `shipments`, `shipment_events`, `drivers`, `vehicles`, `clients`.
