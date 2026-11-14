"""SQLite metadata-only summary: received records, NOT physiological totals."""
from __future__ import annotations
from datetime import datetime, timezone
from pathlib import Path
import sqlite3
from typing import Any


class SummaryPlugin:
    name = "summary"

    def __init__(self, db_path: str):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        with self._connect() as db:
            db.execute("CREATE TABLE IF NOT EXISTS batches (digest TEXT PRIMARY KEY, received_day TEXT NOT NULL)")
            db.execute("""CREATE TABLE IF NOT EXISTS daily_counts (
                day TEXT NOT NULL, data_type TEXT NOT NULL, records_received INTEGER NOT NULL,
                PRIMARY KEY(day, data_type))""")

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self.db_path, timeout=30)

    def handle(self, payload: dict[str, Any], batch_id: str) -> None:
        day = datetime.now(timezone.utc).date().isoformat()
        counts = {name: len(records) for name, records in payload.items() if isinstance(records, list)}
        with self._connect() as db:
            db.execute("BEGIN IMMEDIATE")
            inserted = db.execute(
                "INSERT OR IGNORE INTO batches(digest, received_day) VALUES (?, ?)",
                (batch_id, day),
            )
            if inserted.rowcount == 0:
                return
            for category, count in counts.items():
                db.execute("""INSERT INTO daily_counts(day, data_type, records_received)
                    VALUES (?, ?, ?) ON CONFLICT(day, data_type)
                    DO UPDATE SET records_received = records_received + excluded.records_received""",
                    (day, category, count),
                )

    def report(self, day: str) -> dict[str, Any]:
        with self._connect() as db:
            batches = db.execute("SELECT COUNT(*) FROM batches WHERE received_day = ?", (day,)).fetchone()[0]
            rows = db.execute(
                "SELECT data_type, records_received FROM daily_counts WHERE day = ? ORDER BY data_type",
                (day,),
            ).fetchall()
        return {
            "day_utc": day,
            "accepted_unique_batches": batches,
            "records_received_by_type": dict(rows),
            "note": "Delivery/ingestion counts, not unique measurements, steps, or medical insights.",
        }
