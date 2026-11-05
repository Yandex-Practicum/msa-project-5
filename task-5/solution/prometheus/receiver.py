import json
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        print(json.dumps(json.loads(body), ensure_ascii=False), flush=True)
        self.send_response(200)
        self.end_headers()


HTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
