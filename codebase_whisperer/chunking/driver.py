# codebase_whisperer/chunking/driver.py
from __future__ import annotations
from typing import Any, Dict, List, Optional, Tuple

from .plain import chunk_plain
from .t_sitter import core as tcore            # ← import the module, not the functions
from .t_sitter.lang_nodes import LANG_NODE_MAP
from .t_sitter.parser import get_ts_parser

Symbol = Optional[str]
Chunk = Tuple[Symbol, str]

__all__ = ["ts_supported", "get_parser", "chunk_text"]

def ts_supported(lang: str) -> bool:
    if not lang:
        return False
    return (lang.split(".")[0] in LANG_NODE_MAP)

def get_parser(lang: str, cache: Optional[Dict[str, Any]] = None) -> Any:
    short = (lang or "text").split(".")[0]
    if not ts_supported(short):
        return None

    cache = cache if cache is not None else {}
    parser = cache.get(short)
    if parser is not None:
        return parser

    parser_obj, _lang = get_ts_parser(short)
    if parser_obj:
        cache[short] = parser_obj
    return parser_obj

def chunk_text(
    *,
    lang: str,
    text: str,
    max_chunk_chars: int,
    min_chunk_chars: int = 0,
    ts_parser_cache: Optional[Dict[str, Any]] = None,
) -> List[Chunk]:
    """
    Single entry point for chunking.
    - TS-supported langs: tcore.extract_defs -> tcore.chunk_defs_with_limits
    - Otherwise: chunk_plain
    Returns: List[(symbol|None, chunk_text)]
    """
    short = (lang or "text").split(".")[0]
    ts_parser_cache = ts_parser_cache or {}

    if ts_supported(short):
        parser = get_parser(short, ts_parser_cache)
        if parser is None:
            pieces = chunk_plain(text, max_chars=max_chunk_chars, min_chars=min_chunk_chars)
            return [(None, p) for p in pieces]

        # NOTE: call through the module so tests can monkeypatch tcore.extract_defs
        defs: List[Tuple[str, str]] = tcore.extract_defs(short, parser, text)

        if not defs:
            pieces = chunk_plain(text, max_chars=max_chunk_chars, min_chars=min_chunk_chars)
            return [(None, p) for p in pieces]

        out: List[Chunk] = []
        for piece, sym in tcore.chunk_defs_with_limits(defs, max_chunk_chars):
            out.append((sym, piece))
        return out

    pieces = chunk_plain(text, max_chars=max_chunk_chars, min_chars=min_chunk_chars)
    return [(None, p) for p in pieces]