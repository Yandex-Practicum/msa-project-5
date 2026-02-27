# Реализация Distributed Scheduling с k8s CronJob

## Подготовка

Запуск MiniKube:

```shell
minikube start
```

Добавление репозитория Helm и развертывание Postgres:

```shell
helm install my-postgres oci://registry-1.docker.io/bitnamicharts/postgresql \
  --set global.postgresql.auth.username=postgres \
  --set global.postgresql.auth.password=postgres \
  --set global.postgresql.auth.database=logistics
```

Инициализация базы данных:

```shell
kubectl cp init-db.sql default/my-postgres-postgresql-0:/tmp/init-db.sql
kubectl exec -it my-postgres-postgresql-0 -- psql -U postgres -d logistics -f /tmp/init-db.sql
```

Сборка образа программы:

```shell
eval $(minikube docker-env)
docker build -t data-exporter:latest .
```

Создание namespace:
```shell
kubectl create namespace logistics
```

Создание PVC для хранилища файлов:

```shell
kubectl apply -f k8s-pvc.yaml
```

## Тестовый запуск задания

Создание job:

```shell
kubectl apply -f k8s-job.yaml
kubectl get jobs
```
```
NAME              STATUS     COMPLETIONS   DURATION   AGE
data-export-job   Complete   1/1           4s         2m18s
```

Посмотреть лог программы:

```shell
kubectl logs job/data-export-job
```
```
Экспортировано 3 строк из shipments в /data/exports/shipments-20260227-095954.csv
```

Посмотреть сформированный файл (minikube):

```shell
minikube ssh cat /data/exports/shipments-20260227-095954.csv
```
```
id,driver_id,origin,destination,amount
1,1,Москва,Тверь,50000.00
2,1,СПб,Новгород,40000.00
3,1,СПб,Москва,35000.00
```

## Запуск задания по расписанию

Создание CronJob 

```shell
kubectl apply -f k8s-cronjob.yaml
kubectl get cronjobs -n logistics
```
```
NAME                SCHEDULE     SUSPEND   ACTIVE   LAST SCHEDULE   AGE
daily-data-export   0 14 * * *   False     1        7m31s           11m
```

Найдем job, созданный для cronjob:

```shell
kubectl get jobs
```
```
NAME                           STATUS     COMPLETIONS   DURATION   AGE
data-export-cronjob-29581970   Complete   1/1           3s         5m11s
data-export-job                Complete   1/1           4s         15m
```

Посмотрим лог программы:

```shell
kubectl logs job/data-export-cronjob-29581970
```
```
Экспортировано 3 строк из shipments в /data/exports/shipments-20260227-141001.csv
```

Посмотрим сформированный файл (minikube):

```shell
minikube ssh cat /tmp/hostpath-provisioner/default/export-data-pvc/shipments-20260227-141001.csv
```
```
id,driver_id,origin,destination,amount
1,1,Москва,Тверь,50000.00
2,1,СПб,Новгород,40000.00
3,1,СПб,Москва,35000.00
```