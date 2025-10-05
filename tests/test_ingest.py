# tests/test_ingest.py
import os
import tempfile
from pathlib import Path
import pytest

import codebase_whisperer.pipelines.ingest as ingest


class DummyClient:
    def __init__(self, *args, **kwargs):
        # Default to 768 unless a test config explicitly sets dim
        self.calls = []
        self.dim = kwargs.get("dim", 768)

    def embed(self, model, inputs):
        self.calls.append((model, inputs))
        return [[0.1] * self.dim for _ in inputs]


@pytest.fixture
def tmp_repo(tmp_path: Path):
    # make a mini repo with one file that matches default include_globs
    f = tmp_path / "pom.xml"
    f.write_text("<project/>", encoding="utf-8")
    return tmp_path


def test_ingest_smoke(monkeypatch, tmp_repo, tmp_path):
    """End-to-end: ensure ingest runs and writes something."""
    db_dir = tmp_path / "db"
    (tmp_repo / "pom.xml").write_text("<project/>", encoding="utf-8")
    # Monkeypatch black boxes
    monkeypatch.setattr(ingest, "OllamaClient", lambda *a, **kw: DummyClient())

    # Run
    ingest.run_ingest(
        repo_root=str(tmp_repo),
        db_dir=str(db_dir),
        table_name="chunks",
    )

    # DB should exist
    assert (db_dir / "chunks.lance").exists()


def test_ingest_respects_cache(monkeypatch, tmp_repo, tmp_path):
    """Embedding is skipped if cached vector exists."""
    db_dir = tmp_path / "db"
    (tmp_repo / "src").mkdir(parents=True, exist_ok=True)
    (tmp_repo / "src/Foo.java").write_text("class Foo {}", encoding="utf-8")
    dummy = DummyClient()
    monkeypatch.setattr(ingest, "OllamaClient", lambda *a, **kw: dummy)

    # First run should call embed
    ingest.run_ingest(repo_root=str(tmp_repo), db_dir=str(db_dir), table_name="chunks")
    assert dummy.calls, "First run should call embed"

    # Reset and run again (no force_reembed)
    dummy.calls.clear()
    ingest.run_ingest(repo_root=str(tmp_repo), db_dir=str(db_dir), table_name="chunks")
    assert not dummy.calls, "Second run should hit cache, no embed calls"


def test_ingest_force_reembed(monkeypatch, tmp_repo, tmp_path):
    """Force flag bypasses cache and re-calls embed."""
    db_dir = tmp_path / "db"
    (tmp_repo / "src").mkdir(parents=True, exist_ok=True)
    (tmp_repo / "src/Foo.java").write_text("class Foo {}", encoding="utf-8")
    dummy = DummyClient()
    monkeypatch.setattr(ingest, "OllamaClient", lambda *a, **kw: dummy)

    # First run populates cache
    ingest.run_ingest(repo_root=str(tmp_repo), db_dir=str(db_dir), table_name="chunks")

    dummy.calls.clear()
    ingest.run_ingest(
        repo_root=str(tmp_repo),
        db_dir=str(db_dir),
        table_name="chunks",
        force_reembed=True,
    )
    assert dummy.calls, "With force_reembed=True we should call embed again"


def test_ingest_honors_config_file(monkeypatch, tmp_repo, tmp_path):
    """Override config via config file path."""
    db_dir = tmp_path / "db"
    (tmp_repo / "note.txt").write_text("hello via config", encoding="utf-8")
    cfg_file = tmp_path / "cfg.yaml"
    cfg_file.write_text(
        "embedding:\n  model: fake-embed\n  dim: 3\nindexing:\n  include_globs: ['*.txt']\n",
        encoding="utf-8",
    )

    dummy = DummyClient(dim = 3)
    monkeypatch.setattr(ingest, "OllamaClient", lambda *a, **kw: dummy)

    ingest.run_ingest(
        repo_root=str(tmp_repo),
        db_dir=str(db_dir),
        table_name="chunks",
        config_path=str(cfg_file),
    )

    assert any(call[0] == "fake-embed" for call in dummy.calls)