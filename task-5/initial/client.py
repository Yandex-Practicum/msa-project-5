#!/usr/bin/env python3
"""
Клиент для вызова API запуска ETL-задачи.
Использует W3C Trace Context для передачи traceId.
"""

import requests
import uuid
import json
from datetime import datetime

API_BASE_URL = "http://localhost:8080/api/jobs"

def generate_traceparent():
    """Генерирует W3C Trace Context header"""
    trace_id = uuid.uuid4().hex  # 32 hex chars
    span_id = uuid.uuid4().hex[:16]  # 16 hex chars
    return f"00-{trace_id}-{span_id}-01", trace_id, span_id

def start_etl_job(file_name="product-data.csv"):
    """Вызывает API для запуска ETL задачи"""
    traceparent, trace_id, span_id = generate_traceparent()

    headers = {
        "Content-Type": "application/json",
        "traceparent": traceparent,
        "X-Request-Id": trace_id
    }

    url = f"{API_BASE_URL}/process-file"

    print(f"{'='*60}")
    print(f"[{datetime.now().isoformat()}] Starting ETL Job")
    print(f"Trace ID: {trace_id}")
    print(f"Span ID:  {span_id}")
    print(f"URL:      {url}")
    print(f"{'='*60}")

    try:
        response = requests.post(url, params={"fileName": file_name},
                                headers=headers, timeout=30)

        result = response.json()
        print(f"\n📊 Response Status: {response.status_code}")
        print(f"📝 Response Body:\n{json.dumps(result, indent=2, default=str)}")

        if response.status_code == 202:
            print(f"\n✅ Job Accepted!")
            print(f"   🔗 Jaeger: http://localhost:16686/trace/{trace_id}")
            print(f"   📋 Kibana: http://localhost:5601 (search traceId: {trace_id})")

        return result, trace_id

    except Exception as e:
        print(f"\n❌ Error: {e}")
        return None, trace_id

if __name__ == "__main__":
    result, trace_id = start_etl_job()

    if result:
        print(f"\n{'='*60}")
        print(f"📌 View trace in Jaeger:")
        print(f"   http://localhost:16686/trace/{trace_id}")
        print(f"📌 View logs in Kibana:")
        print(f"   Search: traceId: {trace_id}")
        print(f"{'='*60}")