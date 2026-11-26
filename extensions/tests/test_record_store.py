"""Opt-in record storage: updates, repeated batches, deletions and privacy."""
from __future__ import annotations
from pathlib import Path
import sqlite3
import sys
from tempfile import TemporaryDirectory
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from hc_plugins.plugins.record_store import RecordStorePlugin


def entry(value, modified="2026-11-13T10:00:00Z", item_id="r-1"):
    return {
        "count": value,
        "start_time": "2026-11-13T09:00:00Z",
        "end_time": "2026-11-13T10:00:00Z",
        "metadata": {
            "id": item_id,
            "data_origin": "com.example.health",
            "last_modified_time": modified,
        },
    }


class RecordStoreTests(unittest.TestCase):
    def setUp(self):
        self.folder = TemporaryDirectory()
        self.path = str(Path(self.folder.name) / "records.sqlite3")
        self.store = RecordStorePlugin(self.path)

    def tearDown(self):
        self.folder.cleanup()

    def read_value(self):
        with sqlite3.connect(self.path) as db:
            row = db.execute("SELECT raw_json FROM records WHERE data_type='steps' AND record_key='id:r-1'").fetchone()
            return row[0] if row else None

    def test_batch_retry_and_same_records_in_new_envelope(self):
        self.store.handle({"timestamp": "t1", "steps": [entry(15)]}, "batch1")
        self.store.handle({"timestamp": "t1", "steps": [entry(15)]}, "batch1")
        self.store.handle({"timestamp": "t2", "steps": [entry(15)]}, "batch2")
        self.assertEqual(self.store.stats()["stored_records_by_type"]["steps"], 1)
        self.assertEqual(self.store.stats()["processed_batches"], 2)

    def test_edit_replaces_record_and_old_retry_cannot_undo_it(self):
        self.store.handle({"steps": [entry(15)]}, "a")
        self.store.handle({"steps": [entry(30, "2026-11-14T10:00:00Z")]}, "b")
        self.store.handle({"steps": [entry(15)]}, "c")
        self.assertIn('"count":30', self.read_value())

    def test_deletion_removes_record_and_prevents_stale_resurrection(self):
        self.store.handle({"steps": [entry(15)]}, "a")
        self.store.handle({"deleted": [{"type": "steps", "id": "r-1"}]}, "b")
        self.assertIsNone(self.read_value())
        self.assertEqual(self.store.stats()["tombstone_ids"], 1)
        self.store.handle({"steps": [entry(15)]}, "c")
        self.assertIsNone(self.read_value())

    def test_delete_and_update_in_same_batch_delete_wins(self):
        self.store.handle({"steps": [entry(15)], "deleted": [{"type": "steps", "id": "r-1"}]}, "same")
        self.assertIsNone(self.read_value())

    def test_anonymous_aggregate_replaces_by_bucket_start(self):
        self.store.handle({"heart_rate": [{"time": "2026-11-13T10:00:00Z", "avg": 80}]}, "a")
        self.store.handle({"heart_rate": [{"time": "2026-11-13T10:00:00Z", "avg": 82}]}, "b")
        self.assertEqual(self.store.stats()["stored_records_by_type"]["heart_rate"], 1)

    def test_anonymous_raw_uses_fingerprint(self):
        same = {"steps": [{"start_time": "2026-11-13T00:00:00Z", "count": 4}]}
        self.store.handle(same, "a")
        self.store.handle(same, "b")
        self.assertEqual(self.store.stats()["stored_records_by_type"]["steps"], 1)

    def test_malformed_deletion_rolls_back_whole_batch(self):
        with self.assertRaises(ValueError):
            self.store.handle({"steps": [entry(15)], "deleted": [{"type": "steps"}]}, "bad")
        self.assertEqual(self.store.stats()["processed_batches"], 0)
        self.assertEqual(self.store.stats()["stored_records_by_type"], {})


if __name__ == "__main__":
    unittest.main()
