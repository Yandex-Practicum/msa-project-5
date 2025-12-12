## ⚠️ Important

The project is based on the outcome achieved in task-4 and a template provided for task-5.

## Metrics to gather

💡 Metrics export is tuned up through application.properties

1. standard `JVM` metrics (cpu/memory usage and vital system indicators);
2. `Spring Batch` specific metrics (typical job behavior);
3. `Database` metrics (e.g. a number of active connections).

A combination of these 2 categories makes it possible to track `4 golden signals`:
* Latency;
* Throughput;
* Saturation;
* Errors.

These signals combined with threshold alerts:
* could give a head start for an SLI dashboard;
* suit needs of the:
  - SRE team;
  - business folks.

## Logs transmission details

`Filebeat` is chosen for sending logs to `Logstash`, because it:
* is reliable, lightweight and container-oriented;
* has built-in backpressure mechanisms to ensure a delivery guarantee;
* can augment application logs with a container's metadata.
`JSON` logs format is chosen since it's a human-readable, serializable and parsable message format.
`ELK` then serves as a logs frontstore providing a flexible way of searching and visualizing the logs through a comfortable `ui`.

## Demo

A video illustrating a proof of concept in action: https://disk.yandex.ru/i/zgc-GL20lvZLZg