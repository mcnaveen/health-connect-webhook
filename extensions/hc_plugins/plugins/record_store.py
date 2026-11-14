"""Opt-in SQLite storage for Health Connect JSON records.

Raw health records are sensitive: this plugin MUST NOT be enabled by default.
It uses record IDs for deduplication, upserts edits, and applies deletion events.
"""
from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha256
import json
from pathlib import Path
import sqlite3
from typing import Any


class RecordStorePlugin:
    name = "record_store"

    def __init__(self, db_path: str):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        with self._connect() as db:
            db.execute("""CREATE TABLE IF NOT EXISTS records (
                data_type TEXT NOT NULL,
                record_key TEXT NOT NULL,
                identity_kind TEXT NOT NULL,
                data_origin TEXT,
                modified_epoch REAL,
                raw_json TEXT NOT NULL,
                PRIMARY KEY(data_type, record_key)
            )""")
            db.execute("""CREATE TABLE IF NOT EXISTS tombstones (
                data_type TEXT NOT NULL,
                record_id TEXT NOT NULL,
                PRIMARY KEY(data_type, record_id)
            )""")
            db.execute("CREATE TABLE IF NOT EXISTS ingested_batches (digest TEXT PRIMARY KEY)")
            db.execute("CREATE INDEX IF NOT EXISTS ix_records_data_type ON records(data_type)")

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self.db_path, timeout=30)

    @staticmethod
    def _modified_epoch(value: Any) -> float | None:
        if not isinstance(value, str):
            return None
        try:
            parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
            if parsed.tzinfo is None:
                return None
            return parsed.astimezone(timezone.utc).timestamp()
        except (ValueError, OverflowError):
            return None

    @staticmethod
    def _record_key(record: dict[str, Any]) -> tuple[str, str]:
        meta = record.get("metadata") or {}
        if not isinstance(meta, dict):
            meta = {}
        raw_id = meta.get("id")
        if isinstance(raw_id, str) and raw_id:
            return f"id:{raw_id}", "id"

        # Upstream docs: aggregate updates may be identified by type + time.
        is_aggregate = any(
            name in record for name in
            ("avg", "min", "max", "avg_delta_celsius")
        )
        point = record.get("time") or record.get("start_time")
        if is_aggregate and isinstance(point, str) and point:
            return f"bucket:{point}", "aggregate"

        # No record ID: preserve repeated identical records, but cannot
        # reliably recognize edits of anonymous records.
        canonical = json.dumps(record, sort_keys=True, ensure_ascii=False, separators=(",", ":"))
        return f"hash:{sha256(canonical.encode('utf-8')).hexdigest()}", "fingerprint"

    def handle(self, payload: dict[str, Any], batch_id: str) -> None:
        with self._connect() as db:
            db.execute("BEGIN IMMEDIATE")
            created = db.execute(
                "INSERT OR IGNORE INTO ingested_batches(digest) VALUES(?)", (batch_id,)
            )
            if created.rowcount == 0:
                return

            # Insert/update first so deletion events always take precedence
            # if both happen to occur within the same batch.
            for data_type, values in payload.items():
                if data_type == "deleted" or not isinstance(values, list):
                    continue
                for record in values:
                    if not isinstance(record, dict):
                        raise ValueError("Expected an object for each record")
                    key, identity_kind = self._record_key(record)
                    meta = record.get("metadata")
                    if not isinstance(meta, dict):
                        meta = {}
                    if identity_kind == "id":
                        blocked = db.execute(
                            "SELECT 1 FROM tombstones WHERE data_type=? AND record_id=?",
                            (data_type, key[3:]),
                        ).fetchone()
                        if blocked:
                            # Avoid resurrecting a deleted item from delayed retries.
                            # A full historical rebuild requires a separate tool.
                            continue
                    source = meta.get("data_origin")
                    if not isinstance(source, str):
                        source = None
                    modified = self._modified_epoch(meta.get("last_modified_time"))
                    serialized = json.dumps(record, ensure_ascii=False, separators=(",", ":"))
                    db.execute("""INSERT INTO records(
                        data_type, record_key, identity_kind, data_origin, modified_epoch, raw_json
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(data_type, record_key) DO UPDATE SET
                        identity_kind=excluded.identity_kind,
                        data_origin=excluded.data_origin,
                        modified_epoch=excluded.modified_epoch,
                        raw_json=excluded.raw_json
                    WHERE records.modified_epoch IS NULL
                       OR (excluded.modified_epoch IS NOT NULL
                           AND excluded.modified_epoch >= records.modified_epoch)
                    """, (data_type, key, identity_kind, source, modified, serialized))

            for deletion in payload.get("deleted", []):
                if not isinstance(deletion, dict):
                    raise ValueError("Invalid deletion event")
                kind, raw_id = deletion.get("type"), deletion.get("id")
                if not isinstance(kind, str) or not kind or not isinstance(raw_id, str) or not raw_id:
                    raise ValueError("Deletion event requires type and id")
                db.execute(
                    "DELETE FROM records WHERE data_type=? AND record_key=?",
                    (kind, "id:" + raw_id),
                )
                db.execute(
                    "INSERT OR IGNORE INTO tombstones(data_type, record_id) VALUES (?, ?)",
                    (kind, raw_id),
                )

    def stats(self) -> dict[str, Any]:
        with self._connect() as db:
            rows = db.execute(
                "SELECT data_type, COUNT(*) FROM records GROUP BY data_type ORDER BY data_type"
            ).fetchall()
            deleted = db.execute("SELECT COUNT(*) FROM tombstones").fetchone()[0]
            batches = db.execute("SELECT COUNT(*) FROM ingested_batches").fetchone()[0]
        return {
            "storage_enabled": True,
            "stored_records_by_type": dict(rows),
            "tombstone_ids": deleted,
            "processed_batches": batches,
            "warning": "These are record counts, not deduplicated physiological totals.",
        }
