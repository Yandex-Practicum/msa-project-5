# C4 — мониторинг и логи (дополнение)

```mermaid
flowchart LR
  app[Spring_Batch_app]
  prom[Prometheus]
  graf[Grafana]
  fb[Filebeat]
  es[Elasticsearch]
  kb[Kibana]

  app -->|metrics_8080| prom
  prom --> graf
  app -->|stdout_JSON| fb
  fb --> es
  es --> kb
```
