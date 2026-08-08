# Task 3: K8s CronJob для экспорта данных грузоперевозок

### 1. Запуск Minikube

```bash
minikube start --driver=docker --driver=docker
```

### 2. Сборка Docker-образа и загрузка в Minikube

Сборка выполняется на хосте (в docker-демоне minikube на WSL2 возможны проблемы с DNS при `pip install`), затем образ загружается в minikube:

```bash
# из директории results/ (Dockerfile берёт код из app/)
docker build -t events-exporter:1.0 .
minikube image load events-exporter:1.0

# проверка, что образ виден внутри minikube
minikube image ls | grep events-exporter
```

### 3. Применение K8s-манифестов

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/postgres-init-configmap.yaml
kubectl apply -f k8s/postgres-deployment.yaml

# Дождаться готовности PostgreSQL
kubectl wait --namespace logistics-export --for=condition=ready pod -l app=postgres --timeout=300s

kubectl apply -f k8s/export-pvc.yaml
kubectl apply -f k8s/cronjob.yaml
```

### 4. Ручной запуск Job (не дожидаясь крона)

```bash
kubectl create job --from=cronjob/shipment-events-export test-export -n logistics-export
```

### 5. Проверка результата

```bash
# Логи Job
kubectl logs -n logistics-export job/test-export

# Проверка CSV в PVC
kubectl run --rm -it check-csv -n logistics-export \
  --image=busybox \
  --overrides='{
    "spec": {
      "containers": [{
        "name": "check",
        "image": "busybox",
        "command": ["sh", "-c", "ls -la /export && head -5 /export/*.csv"],
        "volumeMounts": [{"name": "export", "mountPath": "/export"}]
      }],
      "volumes": [{
        "name": "export",
        "persistentVolumeClaim": {"claimName": "export-data"}
      }],
      "restartPolicy": "Never"
    }
  }'
```

### 6. Состояние CronJob

```bash
kubectl get cronjob -n logistics-export
kubectl get jobs -n logistics-export
kubectl get pods -n logistics-export
```

## Конфигурация

Расписание и параметры задаются в `k8s/cronjob.yaml`:

- `schedule: "0 20 * * *"` + `timeZone: "Europe/Moscow"` — каждый день в 20:00 по Москве
- `backoffLimit: 2` — 2 повторные попытки при ошибке
- `activeDeadlineSeconds: 3600` — максимум 1 час на выгрузку
- `concurrencyPolicy: Forbid` — не запускать новый Job, пока предыдущий работает
- `successfulJobsHistoryLimit: 5` / `failedJobsHistoryLimit: 2` — история запусков

Экспортируемая таблица — `shipment_events` (скрипт `app/export_events.py`). Подключение к БД настраивается через `k8s/configmap.yaml` (хост, порт, база, каталог выгрузки) и `k8s/secret.yaml` (пользователь, пароль).
