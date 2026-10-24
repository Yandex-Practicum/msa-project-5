# Трейсинг запросов

Spring Boot и Micrometer Tracing с Brave создают traceId и spanId для HTTP-запроса. Контроллер добавляет URI в MDC на время запуска Spring Batch. Logstash Encoder записывает контекст в JSON; Filebeat и Logstash передают его в Elasticsearch. В Kibana записи одного запроса можно найти по traceId.
