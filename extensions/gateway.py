"""Standard-library JSON webhook receiver and server-side plugin host."""
from __future__ import annotations
from datetime import date, datetime, timezone
from hashlib import sha256
from hmac import compare_digest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import logging
import os
from typing import Any
from urllib.parse import parse_qs, urlsplit

from hc_plugins.core import PluginManager
from hc_plugins.plugins.summary import SummaryPlugin

MAX_BODY_BYTES_DEFAULT = 16 * 1024 * 1024
METADATA_KEYS = {"timestamp", "app_version", "timezone"}


def validate_payload(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValueError("Root must be an object")
    if not isinstance(value.get("timestamp"), str) or not value["timestamp"]:
        raise ValueError("Missing timestamp")
    if not isinstance(value.get("app_version"), str) or not value["app_version"]:
        raise ValueError("Missing app_version")
    if "timezone" in value and not isinstance(value["timezone"], str):
        raise ValueError("Invalid timezone")
    for name, records in value.items():
        if name not in METADATA_KEYS and not isinstance(records, list):
            raise ValueError("Record fields must contain arrays")
    return value


def build_handler(manager: PluginManager, api_key: str, max_body_bytes: int = MAX_BODY_BYTES_DEFAULT):
    if not api_key:
        raise ValueError("GATEWAY_API_KEY is required")
    if max_body_bytes <= 0:
        raise ValueError("max_body_bytes must be positive")

    class Handler(BaseHTTPRequestHandler):
        server_version = "HCPluginGateway/0.1"

        def _reply(self, status: int, payload: dict[str, Any]) -> None:
            body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(body)

        def _authorized(self) -> bool:
            provided = self.headers.get("x-api-key", "")
            return compare_digest(provided, api_key)

        def do_POST(self) -> None:
            if urlsplit(self.path).path != "/webhook":
                return self._reply(404, {"error": "Not found"})
            if not self._authorized():
                return self._reply(401, {"error": "Unauthorized"})
            if self.headers.get("Transfer-Encoding"):
                return self._reply(400, {"error": "Transfer-Encoding is unsupported"})
            if self.headers.get("Content-Type", "").split(";")[0].strip().lower() != "application/json":
                return self._reply(415, {"error": "Expected application/json"})
            try:
                length = int(self.headers["Content-Length"])
            except (KeyError, ValueError, TypeError):
                return self._reply(411, {"error": "Content-Length required"})
            if length < 0 or length > max_body_bytes:
                self.close_connection = True
                return self._reply(413, {"error": "Payload too large"})
            try:
                payload = validate_payload(json.loads(self.rfile.read(length)))
            except (ValueError, UnicodeDecodeError, json.JSONDecodeError):
                return self._reply(400, {"error": "Invalid JSON health payload"})
            # Canonical JSON makes byte-for-byte retransmission order-independent.
            batch_id = sha256(json.dumps(payload, sort_keys=True, separators=(",", ":"),
                                          ensure_ascii=False).encode("utf-8")).hexdigest()
            try:
                manager.process(payload, batch_id)
            except Exception as exc:
                # Never expose health payloads or exception text to logs or clients.
                logging.error("Plugin dispatch failed (%s)", type(exc).__name__)
                return self._reply(503, {"error": "Plugin processing failed; retry later"})
            return self._reply(200, {"accepted": True})

        def do_GET(self) -> None:
            parsed = urlsplit(self.path)
            if parsed.path == "/health":
                return self._reply(200, {"status": "ok"})
            if not self._authorized():
                return self._reply(401, {"error": "Unauthorized"})
            if parsed.path == "/plugins":
                return self._reply(200, {"plugins": manager.names()})
            if parsed.path == "/reports/daily":
                day = parse_qs(parsed.query).get("day", [datetime.now(timezone.utc).date().isoformat()])[0]
                try:
                    if date.fromisoformat(day).isoformat() != day:
                        raise ValueError("Invalid date")
                except ValueError:
                    return self._reply(400, {"error": "day must be YYYY-MM-DD"})
                try:
                    return self._reply(200, manager.get("summary").report(day))
                except (KeyError, Exception) as exc:
                    logging.error("Report unavailable (%s)", type(exc).__name__)
                    return self._reply(503, {"error": "Report unavailable"})
            return self._reply(404, {"error": "Not found"})

        def log_message(self, fmt: str, *args: Any) -> None:
            # The request path could carry sensitive query parameters; omit access logs.
            pass

    return Handler


def main() -> None:
    os.umask(0o077)
    key = os.environ.get("GATEWAY_API_KEY", "")
    if len(key) < 24:
        raise SystemExit("Set GATEWAY_API_KEY to a random secret of at least 24 characters")
    db_path = os.environ.get("GATEWAY_DB", ".data/summary.sqlite3")
    manager = PluginManager([SummaryPlugin(db_path)])
    host = os.environ.get("GATEWAY_HOST", "127.0.0.1")
    port = int(os.environ.get("GATEWAY_PORT", "8788"))
    max_bytes = int(os.environ.get("GATEWAY_MAX_BODY_BYTES", str(MAX_BODY_BYTES_DEFAULT)))
    with ThreadingHTTPServer((host, port), build_handler(manager, key, max_bytes)) as server:
        logging.warning("Plugin gateway listening on %s:%s", host, port)
        server.serve_forever()


if __name__ == "__main__":
    main()
