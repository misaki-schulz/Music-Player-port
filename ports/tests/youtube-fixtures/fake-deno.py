#!/usr/bin/env python3
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from time import sleep
import os

root = Path.cwd().parents[1]
is_cipher = Path.cwd().parent.name == 'yt-cipher'
gate = root / ('cipher-go' if is_cipher else 'pot-go')
while not gate.exists():
    sleep(0.02)
if gate.read_text() == 'fail':
    raise SystemExit('fixture start failed')

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        self.rfile.read(int(self.headers['Content-Length']))
        if self.headers['Authorization'] != os.environ['API_TOKEN']:
            self.send_error(403)
            return
        if is_cipher and (root / 'cipher-broken').exists():
            self.send_response(500)
            body = b'{"error":"a.addOnDisposeCallback is not a function"}'
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        if self.path == '/generate':
            if not (root / 'warmup-started').exists():
                (root / 'warmup-started').touch()
                while not (root / 'warmup-go').exists():
                    sleep(0.02)
            body = b'{"poToken":"local-pot","contentBinding":"test"}'
        elif self.path == '/get_sts':
            body = b'{"sts":"local"}'
        elif self.path == '/resolve_url':
            body = b'{"resolved_url":"https://test.invalid/local"}'
        else:
            self.send_error(404)
            return
        self.send_response(200)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

with HTTPServer(('127.0.0.1', int(os.environ['PORT'])), Handler) as server:
    print('Server listening on' if is_cipher else 'api listening on', flush=True)
    server.serve_forever()
