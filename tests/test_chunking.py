# tests/test_chunk_text.py
from __future__ import annotations

import re
from typing import Any, List, Tuple

import pytest

# Import the module under test
from codebase_whisperer.chunking import chunk_text

# ---------- helpers ----------
class DummyParser:
    def parse(self, *_args, **_kwargs):
        class _Tree:  # minimal shape if anything introspects
            root_node = None
        return _Tree()



def para(*lines: str) -> str:
    """Join lines with newlines, add a trailing blank line to separate paragraphs."""
    return "\n".join(lines).strip() + "\n\n"

# ---------- plain-text behavior ----------

def test_plain_text_basic_splitting_and_merge_tails():
    text = (
        para("A" * 50, "B" * 50) +             # ~100 chars paragraph
        para("C" * 2100) +                     # > max_chunk_chars -> will be split
        para("D" * 30)                         # tiny tail -> should be merged up to min_chunk_chars
    )
    max_len = 400
    min_len = 150

    chunks = chunk_text(
        lang="text",
        text=text,
        max_chunk_chars=max_len,
        min_chunk_chars=min_len,
        ts_parser_cache=None,
    )

    # 1) All chunks are at most max_len
    for sym, piece in chunks:
        assert sym is None
        assert len(piece) <= max_len

    # 2) No chunk is an ultra-short tail (ensure merge tail logic kicked in)
    #    (Allow the last chunk to be shorter than min if there's just not enough text.)
    if len(chunks) > 1:
        for sym, piece in chunks[:-1]:
            assert len(piece) >= min_len

    # 3) Concatenation (paragraphs separated by blank lines) roughly preserves content
    recon = "\n\n".join(p for _, p in chunks)
    # normalize multiple blank lines for a robust comparison
    recon_norm = re.sub(r"\n\s*\n+", "\n\n", recon.strip())
    text_norm  = re.sub(r"\n\s*\n+", "\n\n", text.strip())
    assert recon_norm.startswith(text_norm[:200])  # spot-check prefix to avoid exact formatting coupling

# ---------- TS path: symbol propagation & chunk size ----------

def test_ts_path_symbol_propagation(monkeypatch):
    """
    Simulate TS path without installing tree-sitter:
      - Force ts_supported(lang)=True
      - Return a non-None parser
      - Fake extract_defs to emit 2 defs, one long (so it splits), one short
    """
    import codebase_whisperer.chunking.driver as drv
    from codebase_whisperer.chunking.t_sitter import core as tcore

    # 1) make ts_supported return True
    monkeypatch.setattr(drv, "ts_supported", lambda lang: True)

    # 2) get_parser returns a dummy object (truthy)
    monkeypatch.setattr(drv, "get_parser", lambda lang, cache=None: DummyParser())

    # 3) fake extract_defs -> two defs; first very long to force splitting
    long_code = "x" * 1200
    short_code = "print('hi')"

    def fake_extract_defs(lang: str, parser: Any, text: str) -> List[Tuple[str, str]]:
        return [("Foo.bar", long_code), ("Baz", short_code)]

    monkeypatch.setattr(tcore, "extract_defs", fake_extract_defs, raising=True)

    # (We rely on the real chunk_defs_with_limits from tcore.)

    chunks = chunk_text(
        lang="python",
        text="# fake source doesn’t matter; we inject defs",
        max_chunk_chars=200,
        min_chunk_chars=0,
        ts_parser_cache={},
    )

    # Expect multiple chunks; each piece from the first def carries symbol "Foo.bar"
    # and the second def chunk carries "Baz".
    assert len(chunks) >= 2
    # first N-1 chunks will be from long_code
    long_chunks = [c for c in chunks if c[0] == "Foo.bar"]
    short_chunks = [c for c in chunks if c[0] == "Baz"]
    assert len(long_chunks) >= 2
    assert len(short_chunks) == 1
    # size limit respected
    for sym, piece in chunks:
        assert len(piece) <= 200

# ---------- TS parser fallback to plain ----------

def test_ts_path_falls_back_to_plain_when_parser_unavailable(monkeypatch):
    import codebase_whisperer.chunking.driver as drv

    # Pretend we support the lang, but cannot get a parser
    monkeypatch.setattr(drv, "ts_supported", lambda lang: True)
    monkeypatch.setattr(drv, "get_parser", lambda lang, cache=None: None)

    text = para("Hello") + para("World")
    chunks = chunk_text(
        lang="java",
        text=text,
        max_chunk_chars=100,
        min_chunk_chars=10,
        ts_parser_cache=None,
    )

    # Should have taken the plain-text path: symbols are None
    assert len(chunks) >= 1
    for sym, piece in chunks:
        assert sym is None
        assert len(piece) <= 100

# ---------- edge cases ----------

@pytest.mark.parametrize("lang", [None, "", "unknownlang"])
def test_unknown_or_empty_lang_uses_plain(lang):
    chunks = chunk_text(
        lang=lang,  # type: ignore[arg-type]
        text="just some text\n\nand more",
        max_chunk_chars=80,
        min_chunk_chars=20,
        ts_parser_cache=None,
    )
    assert len(chunks) >= 1
    for sym, piece in chunks:
        assert sym is None
        assert len(piece) <= 80

def test_empty_text_returns_empty_list():
    out = chunk_text(lang="text", text="", max_chunk_chars=100, min_chunk_chars=0)
    assert out == []