"""Contract tests: auth, payload validation, retry idempotency, and isolation."""
from __future__ import annotations
import http.client
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
from threading import Thread
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from gateway import build_handler, validate_payload
from hc_plugins.core import PluginManager
from hc_plugins.plugins.summary import SummaryPlugin
from http.server import ThreadingHTTPServer

KEY = "test-secret-keep-it-long-enough-123"
SAMPLE = {"timestamp": "2026-11-13T12:00:00Z", "app_version": "1.9.32",
          "timezone": "UTC", "steps": [{"count": 10}], "deleted": []}


class GatewayTests(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.plugin = SummaryPlugin(str(Path(self.temp.name) / "health-summary.db"))
        manager = PluginManager([self.plugin])
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), build_handler(manager, KEY))
        self.thread = Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=3)
        self.temp.cleanup()

    def request(self, method, path, payload=None, authorized=True, content_type="application/json"):
        conn = http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)
        headers = {"Content-Type": content_type}
        if authorized:
            headers["x-api-key"] = KEY
        body = None if payload is None else json.dumps(payload).encode()
        conn.request(method, path, body=body, headers=headers)
        response = conn.getresponse()
        result = response.status, json.loads(response.read())
        conn.close()
        return result

    def test_requires_authentication(self):
        status, _ = self.request("POST", "/webhook", SAMPLE, authorized=False)
        self.assertEqual(status, 401)
        status, _ = self.request("GET", "/reports/daily", authorized=False)
        self.assertEqual(status, 401)

    def test_accepts_and_deduplicates_identical_retries(self):
        for _ in range(2):
            status, data = self.request("POST", "/webhook", SAMPLE)
            self.assertEqual(status, 200)
            self.assertTrue(data["accepted"])
        status, report = self.request("GET", "/reports/daily")
        self.assertEqual(status, 200)
        self.assertEqual(report["accepted_unique_batches"], 1)
        self.assertEqual(report["records_received_by_type"]["steps"], 1)
        self.assertNotIn("raw_payload", report)
        self.assertNotIn('"count": 10', json.dumps(report))

    def test_rejects_invalid_shape(self):
        status, _ = self.request("POST", "/webhook", {"timestamp": "x", "app_version": "v", "steps": 10})
        self.assertEqual(status, 400)
        with self.assertRaises(ValueError):
            validate_payload([])

    def test_plugin_registry(self):
        status, result = self.request("GET", "/plugins")
        self.assertEqual(status, 200)
        self.assertEqual(result["plugins"], ["summary"])


if __name__ == "__main__":
    unittest.main()
