"""Простой клиент для POST /api/jobs/run (задание 6). Требуется Python 3."""
from __future__ import annotations

import os
import sys
import urllib.error
import urllib.request

# Для Docker: приложение на хосте — localhost/127.0.0.1:8080. Можно переопределить:
#   set BATCH_API_BASE=http://localhost:8080
BASE = os.environ.get("BATCH_API_BASE", "http://127.0.0.1:8080").rstrip("/")


def _read_body(resp: object) -> str:
    raw = resp.read()
    return raw.decode() if raw else ""


def main() -> None:
    health_url = f"{BASE}/actuator/health"
    run_url = f"{BASE}/api/jobs/run"

    # 1) Проверка, что сервер жив (иначе будет connection refused или обрыв).
    try:
        h = urllib.request.Request(health_url, method="GET")
        with urllib.request.urlopen(h, timeout=15) as resp:
            if resp.status != 200:
                print(f"Ожидался 200 от {health_url}, получено {resp.status}", file=sys.stderr)
                sys.exit(1)
            _ = _read_body(resp)
    except urllib.error.URLError as e:
        print(
            f"Не удалось подключиться к {health_url}: {e.reason!r}\n"
            "Поднимите стек: cd task-6/initial && docker compose up --build\n"
            "или запустите приложение локально (БД должна быть доступна по URL из application.properties).",
            file=sys.stderr,
        )
        sys.exit(1)

    # 2) POST без тела — без Expect: 100-continue (urllib с data=b'{}' иногда рвёт соединение с Tomcat).
    req = urllib.request.Request(run_url, method="POST", data=b"")
    req.add_header("Content-Length", "0")

    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            print(_read_body(resp).strip())
    except urllib.error.HTTPError as e:
        err_text = e.read().decode(errors="replace")
        print(f"HTTP {e.code}: {err_text or e.reason}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as e:
        print(
            f"Запрос к {run_url} не удался: {e.reason!r}\n"
            "Если приложение в Docker только что стартовало — дождитесь готовности app/postgres; "
            "проверьте логи: docker compose logs app --tail=80",
            file=sys.stderr,
        )
        sys.exit(1)


if __name__ == "__main__":
    main()
