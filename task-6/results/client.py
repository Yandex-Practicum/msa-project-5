import json
import sys
from urllib.request import Request, urlopen


url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8082/api/jobs/import-products"
request = Request(url, data=b"", method="POST")
with urlopen(request, timeout=120) as response:
    print(json.dumps(json.load(response), ensure_ascii=False))
