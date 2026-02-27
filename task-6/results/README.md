```bash
uv run python client.py
```

Скрипт запущен...
Подключение к http://localhost:8080/api/batch/run...
Статус ответа: 200
Время выполнения: 0.231 сек

Ответ от сервера:
traceId   : 2802011e1d18d3f11c3dd452055e5b91
spanId    : 04422ef40bbce51b
message   : Batch job started successfully
status    : success
timestamp : 1771868710192

Трейс в Jaeger UI: http://localhost:16686/trace/2802011e1d18d3f11c3dd452055e5b91

Batch job успешно запущен!

