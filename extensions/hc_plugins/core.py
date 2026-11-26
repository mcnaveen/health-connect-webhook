"""Small, explicit plugin API. No Android code or dynamic package execution."""
from __future__ import annotations
from copy import deepcopy
from typing import Any, Protocol


class Plugin(Protocol):
    name: str

    def handle(self, payload: dict[str, Any], batch_id: str) -> None:
        """Process one batch. Implementations MUST be idempotent by batch_id."""


class PluginManager:
    def __init__(self, plugins: list[Plugin]):
        names = [p.name for p in plugins]
        if len(names) != len(set(names)):
            raise ValueError("Duplicate plugin name")
        self._plugins = {p.name: p for p in plugins}

    def process(self, payload: dict[str, Any], batch_id: str) -> None:
        # Isolate plugin mutations. Fail closed so the Android sender can retry.
        # A failed plugin can cause a retry after other plugins succeeded;
        # therefore every plugin MUST deduplicate using batch_id.
        for plugin in self._plugins.values():
            plugin.handle(deepcopy(payload), batch_id)

    def names(self) -> list[str]:
        return list(self._plugins)

    def get(self, name: str) -> Plugin:
        return self._plugins[name]
