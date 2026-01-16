import os
import json
import time
import secrets
import argparse
from datetime import datetime, timezone
from urllib.parse import urlparse

import requests


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def gen_trace_id() -> str:
    return secrets.token_hex(16)


def gen_span_id() -> str:
    return secrets.token_hex(8)


def log_json(path: str | None, payload: dict):
    line = json.dumps(payload, ensure_ascii=False)
    if path:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "a", encoding="utf-8") as f:
            f.write(line + "\n")
    else:
        print(line, flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default=os.getenv("API_URL", "http://batch-processing:8080/api/v1/etl/run"))
    parser.add_argument("--log-file", default=os.getenv("LOG_FILE", "/var/log/batch/client.log"))
    parser.add_argument("--timeout", type=int, default=int(os.getenv("TIMEOUT", "30")))
    args = parser.parse_args()

    trace_id = gen_trace_id()
    span_id = gen_span_id()

    headers = {
        "X-B3-TraceId": trace_id,
        "X-B3-SpanId": span_id,
        "X-B3-Sampled": "1",
        "Content-Type": "application/json",
    }

    parsed = urlparse(args.url)
    uri = parsed.path or "/"

    t0 = time.time()
    status = None
    resp_json = None
    err = None

    try:
        r = requests.post(args.url, headers=headers, json={}, timeout=args.timeout)
        status = r.status_code

        try:
            resp_json = r.json()
        except Exception:
            resp_json = {"raw": r.text[:500]}
    except Exception as e:
        err = str(e)

    elapsed_ms = int((time.time() - t0) * 1000)

    event = {
        "@timestamp": now_iso(),
        "service": "etl-client",
        "level": "INFO" if err is None and (status and status < 400) else "ERROR",
        "message": "etl_trigger_request",
        "traceId": trace_id,
        "spanId": span_id,
        "uri": uri,
        "method": "POST",
        "status": status,
        "durationMs": elapsed_ms,
        "apiUrl": args.url,
        "response": resp_json,
        "error": err,
    }

    log_path = args.log_file
    if log_path and os.path.dirname(log_path) == "":
        log_path = None

    try:
        log_json(log_path, event)
    except Exception:
        log_json(None, event)

    if err is not None or (status is not None and status >= 400):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
