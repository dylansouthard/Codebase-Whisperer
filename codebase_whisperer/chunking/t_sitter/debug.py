import os


# codebase_whisperer/chunking/debug.py
from __future__ import annotations
import os
from typing import Iterable

_TRUTHY = {"1", "true", "t", "yes", "y", "on"}

def _env_truth(name: str) -> bool:
    v = os.getenv(name, "")
    return v.lower() in _TRUTHY

def _env_list(name: str) -> set[str]:
    raw = os.getenv(name, "")
    return {p.strip().lower() for p in raw.split(",") if p.strip()}

def debug_enabled(ns: str | None = None) -> bool:
    """
    Returns True if debug logging should be enabled.
    Priority:
      1) DEBUG_ALL truthy => on for all namespaces
      2) DEBUG_<NS> truthy (e.g., DEBUG_CHUNK, DEBUG_TS)
      3) DEBUG contains ns in comma list (e.g., DEBUG=chunk,ts)
    """
    if _env_truth("DEBUG_ALL"):
        return True
    if ns:
        key = f"DEBUG_{ns.upper()}"
        if _env_truth(key):
            return True
        if ns.lower() in _env_list("DEBUG"):
            return True
    return False

def dlog(ns: str, *args, **kwargs) -> None:
    """Namespace-aware debug print."""
    if debug_enabled(ns):
        print(f"[{ns}]", *args, **kwargs)

# ----- handy node helpers for Tree-sitter style debug -----
def node_span(node) -> str:
    """Cheap byte-span preview for a TS node."""
    try:
        return f"[{node.start_byte}:{node.end_byte}]"
    except Exception:
        return "<?>"

def node_preview(utf8: bytes, node, n: int = 60) -> str:
    """Small text preview of a node's source slice (decoded as UTF-8)."""
    try:
        s = utf8[node.start_byte:node.end_byte].decode("utf-8", "replace")
        s = s.replace("\n", "\\n")
        return (s[:n] + "…") if len(s) > n else s
    except Exception:
        return "<decode-failed>"