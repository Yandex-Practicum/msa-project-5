"""
ETL Job Client — вызывает REST API Spring Batch приложения для запуска ETL-задачи.
Передает traceId и spanId через заголовки B3 Propagation для сквозного трейсинга.
"""

import uuid
import random
import time
import logging
import sys

import requests

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] traceId=%(trace_id)s spanId=%(span_id)s — %(message)s",
    stream=sys.stdout,
)


def generate_trace_id() -> str:
    return uuid.uuid4().hex


def generate_span_id() -> str:
    return format(random.getrandbits(64), "016x")


def launch_job(base_url: str) -> None:
    trace_id = generate_trace_id()
    span_id = generate_span_id()

    logger = logging.LoggerAdapter(
        logging.getLogger(__name__),
        {"trace_id": trace_id, "span_id": span_id},
    )

    url = f"{base_url}/api/jobs/import-products"

    # B3 single-header propagation (supported by Brave / Micrometer)
    headers = {
        "b3": f"{trace_id}-{span_id}-1",
        "Content-Type": "application/json",
    }

    logger.info("Sending POST %s", url)

    try:
        resp = requests.post(url, headers=headers, timeout=30)
        resp.raise_for_status()
        body = resp.json()
        logger.info("Job started: jobId=%s, status=%s", body.get("jobId"), body.get("status"))
    except requests.exceptions.ConnectionError:
        logger.error("Cannot connect to %s — is the app running?", url)
    except requests.exceptions.RequestException as exc:
        logger.error("Request failed: %s", exc)


def main() -> None:
    base_url = "http://app:8080"
    max_retries = 12
    retry_delay = 5

    log = logging.LoggerAdapter(
        logging.getLogger(__name__),
        {"trace_id": "-", "span_id": "-"},
    )

    # Wait for the app to become healthy
    for attempt in range(1, max_retries + 1):
        try:
            r = requests.get(f"{base_url}/actuator/health", timeout=5)
            if r.status_code == 200:
                log.info("App is healthy, launching job")
                break
        except requests.exceptions.RequestException:
            pass
        log.info("Waiting for app to start (%d/%d)...", attempt, max_retries)
        time.sleep(retry_delay)
    else:
        log.error("App did not become healthy after %d attempts", max_retries)
        sys.exit(1)

    launch_job(base_url)


if __name__ == "__main__":
    main()
