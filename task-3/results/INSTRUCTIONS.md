# Локальный запуск Task 3

Команды выполняются из корня репозитория.

## 1. Проверка Java-приложения

```bash
mvn -f task-3/results/app/pom.xml clean test
```

## 2. Запуск Minikube и сборка образа

```bash
minikube start -p msa-project-5 --driver=docker
docker build -t shipment-exporter:local task-3/results
minikube -p msa-project-5 image load --overwrite=true shipment-exporter:local
```

## 3. Развёртывание в чистом окружении

Удалите состояние предыдущего демонстрационного запуска. Команда удалит namespace `shipment-export` вместе с его тестовой БД и CSV-файлами в PVC.

```bash
kubectl --context msa-project-5 delete namespace shipment-export \
  --ignore-not-found \
  --wait=true
```

Разверните окружение заново:

```bash
kubectl --context msa-project-5 apply -f task-3/results/k8s/demo-environment.yaml
kubectl --context msa-project-5 -n shipment-export \
  rollout status deployment/shipment-db --timeout=120s
kubectl --context msa-project-5 apply -f task-3/results/k8s/cronjob.yaml
kubectl --context msa-project-5 -n shipment-export get cronjob shipment-export
```

CronJob настроен на ежедневный запуск в 20:00 по часовому поясу `Europe/Moscow`.

## 4. Проверочный запуск без ожидания 20:00

```bash
kubectl --context msa-project-5 -n shipment-export \
  delete job shipment-export-manual --ignore-not-found
kubectl --context msa-project-5 -n shipment-export create job \
  --from=cronjob/shipment-export \
  shipment-export-manual
kubectl --context msa-project-5 -n shipment-export wait \
  --for=condition=complete \
  job/shipment-export-manual \
  --timeout=120s
kubectl --context msa-project-5 -n shipment-export logs job/shipment-export-manual
```

Ожидаемый результат в логах:

```text
Starting shipments export for YYYY-MM-DD
Export completed: rows=12, file=/data/exports/shipments-YYYY-MM-DD.csv
```

## 5. Проверка CSV

```bash
kubectl --context msa-project-5 -n shipment-export \
  delete pod output-inspector --ignore-not-found
kubectl --context msa-project-5 apply -f task-3/results/k8s/output-inspector.yaml
kubectl --context msa-project-5 -n shipment-export wait \
  --for=condition=Ready \
  pod/output-inspector \
  --timeout=120s
kubectl --context msa-project-5 -n shipment-export \
  exec output-inspector -- ls -lh /data/exports
kubectl --context msa-project-5 -n shipment-export exec output-inspector -- \
  sh -c 'head -n 5 /data/exports/*.csv'
```

Для отчёта сделайте скриншоты успешного Job с логами и результата двух последних команд.

## 6. Очистка после демонстрации

```bash
kubectl --context msa-project-5 delete namespace shipment-export
minikube stop -p msa-project-5
```
