# tests/test_indexer.py
from __future__ import annotations
from pathlib import Path
import os
import hashlib
import sys

import pytest

# Import the public API we exposed
from ..indexer import index_repo
from ..languages import EXT_LANGUAGE_MAP, FILENAME_LANGUAGE_MAP

def _write_text(p: Path, text: str, encoding: str = "utf-8"):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding=encoding)

def _sha256_bytes(b: bytes) -> str:
    h = hashlib.sha256()
    h.update(b)
    return h.hexdigest()

def _sha256_text(s: str, encoding="utf-8") -> str:
    return _sha256_bytes(s.encode(encoding))

def test_basic_indexing_includes_excludes_and_lang(tmp_path: Path):
    # tree
    _write_text(tmp_path / "src/main/Foo.java", "class Foo {}", "utf-8")
    _write_text(tmp_path / "target/gen/Bar.java", "class Bar {}", "utf-8")
    _write_text(tmp_path / "pom.xml", "<project/>", "utf-8")

    includes = ["**/*.java", "pom.xml"]
    excludes = ["**/target/**"]

    recs = list(
        index_repo(
            repo_root=str(tmp_path),
            include_globs=includes,
            exclude_globs=excludes,
            encodings=["utf-8"],
            max_file_mb=5,
            include_hidden=True,
            follow_symlinks=False,
        )
    )

    rels = sorted(r.rel_path for r in recs)
    assert rels == ["pom.xml", "src/main/Foo.java"]

    # Language detection: pom.xml via filename override, .java via ext map
    langs = {r.rel_path: r.language for r in recs}
    assert langs["pom.xml"] == "xml"
    assert langs["src/main/Foo.java"] == "java"

    # Text is present
    content_by_rel = {r.rel_path: r.text for r in recs}
    assert content_by_rel["pom.xml"] == "<project/>"
    assert "class Foo" in content_by_rel["src/main/Foo.java"]

def test_sha256_and_paths_and_symlink_flag(tmp_path: Path):
    real = tmp_path / "real.txt"
    _write_text(real, "hello")

    link = tmp_path / "link.txt"
    try:
        link.symlink_to(real)
        symlinks_supported = True
    except (OSError, NotImplementedError):
        symlinks_supported = False

    includes = ["**/*.txt"]
    excludes = []

    # follow_symlinks=True should include both (on platforms that support it)
    recs = list(
        index_repo(
            repo_root=str(tmp_path),
            include_globs=includes,
            exclude_globs=excludes,
            encodings=["utf-8"],
            max_file_mb=5,
            include_hidden=True,
            follow_symlinks=True,
        )
    )

    got = {r.rel_path: r for r in recs}
    assert "real.txt" in got
    if symlinks_supported:
        assert "link.txt" in got
        assert got["link.txt"].is_symlink is True
        # realpath of link should point at real
        assert Path(got["link.txt"].real_path).resolve() == real.resolve()
    # sha256 computed from REAL file bytes
    expect_sha = _sha256_text("hello")
    assert got["real.txt"].sha256 == expect_sha

def test_size_gate_skips_large_files(tmp_path: Path):
    small = tmp_path / "small.txt"
    big = tmp_path / "big.bin"
    _write_text(small, "ok")
    # ~2MB binary-ish file
    big.parent.mkdir(parents=True, exist_ok=True)
    big.write_bytes(b"\x00" * (2 * 1024 * 1024))

    recs = list(
        index_repo(
            repo_root=str(tmp_path),
            include_globs=["**/*"],
            exclude_globs=[],
            encodings=["utf-8"],
            max_file_mb=1,  # 1MB gate
            include_hidden=True,
            follow_symlinks=False,
        )
    )
    rels = sorted(r.rel_path for r in recs)
    assert rels == ["small.txt"]

def test_hidden_file_skip_flag(tmp_path: Path):
    _write_text(tmp_path / "app/app.txt", "A")
    _write_text(tmp_path / "app/.secret", "S")

    # include_hidden=False should drop .secret
    recs = list(
        index_repo(
            repo_root=str(tmp_path),
            include_globs=["**/*"],
            exclude_globs=[],
            encodings=["utf-8"],
            max_file_mb=5,
            include_hidden=False,
            follow_symlinks=False,
        )
    )
    rels = sorted(r.rel_path for r in recs)
    assert rels == ["app/app.txt"]

    # include_hidden=True includes both
    recs2 = list(
        index_repo(
            repo_root=str(tmp_path),
            include_globs=["**/*"],
            exclude_globs=[],
            encodings=["utf-8"],
            max_file_mb=5,
            include_hidden=True,
            follow_symlinks=False,
        )
    )
    rels2 = sorted(r.rel_path for r in recs2)
    assert rels2 == ["app/.secret", "app/app.txt"]

def test_encoding_fallbacks(tmp_path: Path):
    # latin-1 only (contains byte 0xE9)
    p = tmp_path / "notes.txt"
    _write_text(p, "café", encoding="latin-1")

    recs = list(
        index_repo(
            repo_root=str(tmp_path),
            include_globs=["**/*.txt"],
            exclude_globs=[],
            encodings=["utf-8", "latin-1"],  # fallback order
            max_file_mb=5,
            include_hidden=True,
            follow_symlinks=False,
        )
    )
    assert len(recs) == 1
    assert recs[0].text == "café"
    assert recs[0].language == "text"

def test_filename_language_override_beats_extension(tmp_path: Path):
    # Even though it's .xml already, ensure filename override path works too.
    p = tmp_path / "pom.xml"
    _write_text(p, "<project/>")

    recs = list(
        index_repo(
            repo_root=str(tmp_path),
            include_globs=["pom.xml"],
            exclude_globs=[],
            encodings=["utf-8"],
            max_file_mb=5,
            include_hidden=True,
            follow_symlinks=False,
        )
    )
    assert len(recs) == 1
    assert recs[0].language == "xml"