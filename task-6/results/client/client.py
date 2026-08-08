"""
ETL Job Client — вызывает REST API Spring Batch-приложения и запускает ETL-джобу.

Сквозной трейсинг: клиент генерирует traceId/spanId и передаёт их в приложение
заголовком B3 single-header (`b3: {traceId}-{spanId}-1`), который понимает
Micrometer Tracing (Brave). Благодаря этому логи клиента и сервера связываются
одним traceId, а в Zipkin виден единый трейс «клиент → API → джоба».
"""

import logging
import random
import sys
import time
import uuid

import requests

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] traceId=%(trace_id)s spanId=%(span_id)s %(message)s",
    stream=sys.stdout,
)


def new_trace_id() -> str:
    return uuid.uuid4().hex


def new_span_id() -> str:
    return format(random.getrandbits(64), "016x")


def wait_for_app(base_url: str, attempts: int = 18, delay: int = 5) -> None:
    log = logging.LoggerAdapter(logging.getLogger("client"), {"trace_id": "-", "span_id": "-"})
    for attempt in range(1, attempts + 1):
        try:
            if requests.get(f"{base_url}/actuator/health", timeout=5).status_code == 200:
                log.info("Приложение доступно, запускаем ETL-джобу")
                return
        except requests.exceptions.RequestException:
            pass
        log.info("Ждём старта приложения (%d/%d)...", attempt, attempts)
        time.sleep(delay)
    log.error("Приложение не стало доступным за %d попыток", attempts)
    sys.exit(1)


def run_etl_job(base_url: str) -> None:
    trace_id, span_id = new_trace_id(), new_span_id()
    log = logging.LoggerAdapter(
        logging.getLogger("client"), {"trace_id": trace_id, "span_id": span_id}
    )

    url = f"{base_url}/api/etl/run"
    headers = {"b3": f"{trace_id}-{span_id}-1"}

    log.info("POST %s", url)
    try:
        resp = requests.post(url, headers=headers, timeout=120)
        resp.raise_for_status()
        body = resp.json()
        log.info(
            "Джоба выполнена: job=%s executionId=%s status=%s",
            body.get("jobName"), body.get("executionId"), body.get("status"),
        )
        log.info(
            "Найти этот запрос в Kibana можно по запросу: traceId : \"%s\"", trace_id
        )
    except requests.exceptions.RequestException as exc:
        log.error("Запуск джобы не удался: %s", exc)
        sys.exit(1)


def main() -> None:
    base_url = "http://app:8080"
    wait_for_app(base_url)
    run_etl_job(base_url)


if __name__ == "__main__":
    main()
