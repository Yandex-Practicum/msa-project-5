# Task 3 — результаты

Код: [`../export`](../export).

## Порядок запуска (Windows, Docker Desktop Kubernetes или Minikube)

### 1. Собрать образ экспорта

Из каталога `task-3`:

```powershell
cd C:\msa\sprint5\msa-project-5\task-3
docker build -t export-shipments:local .\export
```

### 2. Загрузить образ в кластер (если Job в `ImagePullBackOff`)

- **Minikube:** `minikube image load export-shipments:local`
- **Docker Desktop:** часто локальный `docker build` уже виден ноде; если нет — см. [документацию](https://docs.docker.com/desktop/kubernetes/) (раздел про локальные образы) или временно в `cronjob.yaml` поставьте `imagePullPolicy: Never`.

### 3. Применить секрет, PostgreSQL, CronJob

Важно: **сначала** поднять БД с сервисом **`postgres`**, иначе экспорт не резолвит DNS.

```powershell
cd C:\msa\sprint5\msa-project-5\task-3\export\k8s
kubectl apply -f secret-example.yaml
kubectl apply -f postgres.yaml
kubectl wait --for=condition=available deployment/postgres --timeout=120s
kubectl apply -f cronjob.yaml
```

Файл `postgres.yaml` поднимает PostgreSQL, создаёт БД `demo`, пользователя из secret (`demo`/`demo`) и таблицу `shipments` с тестовыми строками.

### 4. Ручной запуск job

Удалите старый упавший job при необходимости:

```powershell
kubectl delete job manual-run --ignore-not-found
```

Загрузите образ, если ещё не загрузили (см. шаг 2), затем:

```powershell
kubectl create job --from=cronjob/shipments-export manual-run
kubectl logs -f job/manual-run
```

Ожидаемо в логах: `Written 2 rows to /out/shipments.csv`.

### 5. Скриншоты для сдачи

`kubectl get cronjob`, `kubectl get jobs`, `kubectl get pods`, успешные `kubectl logs job/...`.
