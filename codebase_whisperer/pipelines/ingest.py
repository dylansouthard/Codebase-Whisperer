# codebase_whisperer/ingest.py
from __future__ import annotations
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import hashlib
import sys

from rich import print as rprint

from codebase_whisperer.config import load_config
from codebase_whisperer.indexing.indexer import index_repo
from codebase_whisperer.chunking.driver import chunk_text
from codebase_whisperer.llm.ollama import OllamaClient
from codebase_whisperer.logging_utils import StageTimer, CounterBar
from codebase_whisperer.db.io import (
    open_db,
    ensure_chunks,
    ensure_vec_cache,
    load_vec_cache_map,
    validate_vectors,
    upsert_rows,
    ensure_vector_index,
)

def _sha256_text(s: str) -> str:
    return hashlib.sha256(s.encode("utf-8", "replace")).hexdigest()

def run_ingest(
    *,
    repo_root: str,
    db_dir: str,
    table_name: str,
    config_path: Optional[str] = None,
    force_reembed: bool = False,
) -> None:
    """
    High-level pipeline:
      - load config
      - walk & index files (no duplication of indexer defaults)
      - chunk content via chunker
      - embed (using Ollama client defaults unless config overrides)
      - upsert rows to LanceDB through io.py black-box helpers
    """
    with StageTimer("ingest.load_config", extra={"repo_root": str(Path(repo_root).resolve())}):
        cfg, _ = load_config(config_path)

    # --- read-only config (don’t duplicate logic that packages already enforce) ---
    idx = cfg.get("indexing", {})
    include_globs: List[str] = idx.get("include_globs", ["**/*"])
    exclude_globs: List[str] = idx.get("exclude_globs", [])
    encodings: List[str] = idx.get("encodings", ["utf-8"])
    max_file_mb: float   = float(idx.get("max_file_mb", 1.5))
    include_hidden: bool = bool(idx.get("include_hidden", False))
    follow_symlinks: bool = bool(idx.get("follow_symlinks", False))
    max_chunk_chars: int = int(idx.get("max_chunk_chars", 2400))
    min_chunk_chars: int = int(idx.get("min_chunk_chars", 200))

    emb = cfg.get("embedding", {})
    model: str = emb.get("model", "nomic-embed-text")              # Ollama default is still honored inside client
    dim: Optional[int] = emb["dim"] if "dim" in emb else None                         # If absent, we’ll infer from first response
    host: str = emb.get("host", "http://localhost:11434")
    timeout: float = float(emb.get("timeout", 30))
    retries: int = int(emb.get("retries", 2))
    backoff: float = float(emb.get("backoff", 0.2))
    print(f"DEBUG: config requested model={model}, dim={dim}", file=sys.stderr)
    # --- open DB and ensure tables using io.py only ---
    with StageTimer("ingest.db.open", extra={"db_dir": db_dir}):
        db = open_db(db_dir)
    # If dim is unknown, we’ll create tables lazily after first embed.
    chunks_tbl = None
    vcache_tbl = None
    vec_cache: Dict[str, List[float]] = {}

    # If dim is provided in config, we can eagerly wire up tables and load cache now.
    if dim is not None:
        chunks_tbl = ensure_chunks(db, table_name, int(dim))
        vcache_tbl = ensure_vec_cache(db, int(dim))
        vec_cache = load_vec_cache_map(vcache_tbl, model)

    # --- client ---
    client = OllamaClient(host, timeout=timeout, retries=retries, backoff=backoff)

    # --- walk + index ---
    with StageTimer(
        "ingest.index_repo",
        extra={
            "include_globs": ",".join(include_globs) if include_globs else "",
            "exclude_globs": ",".join(exclude_globs) if exclude_globs else "",
            "include_hidden": include_hidden,
            "follow_symlinks": follow_symlinks,
            "max_file_mb": max_file_mb,
        },
    ):
        file_iter = index_repo(
            repo_root=str(Path(repo_root).resolve()),
            include_globs=include_globs,
            exclude_globs=exclude_globs,
            encodings=encodings,
            max_file_mb=max_file_mb,
            include_hidden=include_hidden,
            follow_symlinks=follow_symlinks,
        )

    ts_parser_cache: Dict[str, object] = {}
    pending_rows: List[dict] = []

    # counters for visibility
    file_bar = CounterBar("files", total=None, every=50)
    chunk_bar = CounterBar("chunks", total=None, every=200)
    embed_bar = CounterBar("embeds", total=None, every=100)
    write_bar = CounterBar("writes", total=None, every=500)

    def _ensure_tables_if_needed(vector_dim: int):
        nonlocal chunks_tbl, vcache_tbl, vec_cache, dim
        if chunks_tbl is None:
            chunks_tbl = ensure_chunks(db, table_name, vector_dim)
        if vcache_tbl is None:
            vcache_tbl = ensure_vec_cache(db, vector_dim)
            vec_cache = load_vec_cache_map(vcache_tbl, model)
        dim = vector_dim

    with StageTimer("ingest.process_files"):
        for rec in file_iter:
            print(f"DEBUG: got file {rec.relpath} lang={rec.lang}", file=sys.stderr)
            pieces: List[Tuple[Optional[str], str]] = chunk_text(
                lang=rec.lang or "text",
                text=rec.content or "",
                max_chunk_chars=max_chunk_chars,
                min_chunk_chars=min_chunk_chars,
                ts_parser_cache=ts_parser_cache,
            )
            print(f"DEBUG: pieces={len(pieces)} for {rec.relpath}", file=sys.stderr)  # ADD HERE
            # build rows + embed
            for idx_i, (symbol, piece) in enumerate(pieces):
                chunk_sha = _sha256_text(piece)
                vec: Optional[List[float]] = None

                use_cached = (not force_reembed) and (chunk_sha in vec_cache)
                if use_cached:
                    vec = vec_cache[chunk_sha]
                else:
                    # Embed now (one by one keeps code simple; batch later if needed)
                    v = client.embed(model, [piece])
                    # normalize to List[float]
                    vec0 = v[0] if isinstance(v, list) and v and isinstance(v[0], (list, tuple)) else v
                    vec = [float(x) for x in vec0]
                    # Learn dim & ensure tables only once we know it
                    if dim is None:
                        _ensure_tables_if_needed(len(vec))

                # If dim was provided in config, ensure tables once up-front
                if dim is not None and chunks_tbl is None:
                    _ensure_tables_if_needed(int(dim))

                # build row to match schema.py exactly
                row = {
                    "id": f"{rec.relpath}:{idx_i}",
                    "path": rec.path,
                    "realpath": rec.realpath,
                    "is_symlink": bool(rec.is_symlink),
                    "relpath": rec.relpath,
                    "lang": rec.lang or "text",
                    "symbol": symbol or "",
                    "chunk_idx": idx_i,
                    "content": piece,
                    "sha256": rec.sha256,
                    "content_sha": chunk_sha,
                    "mtime": float(rec.mtime or 0.0),
                    "vector": vec,
                }
                print(f"DEBUG: adding row id={row['id']} vec_len={len(vec) if vec else None}", file=sys.stderr)  # ADD HERE
                pending_rows.append(row)

            # flush opportunistically to keep memory steady
            if len(pending_rows) >= 500:
                _flush_rows(pending_rows, chunks_tbl, vcache_tbl, model, dim)
                if vcache_tbl is not None:
                    vec_cache = load_vec_cache_map(vcache_tbl, model)
    # final flush
    _flush_rows(pending_rows, chunks_tbl, vcache_tbl, model, dim)
    if vcache_tbl is not None:
        vec_cache = load_vec_cache_map(vcache_tbl, model)
    # create vector index lazily (no-op on empty)
        # create vector index lazily (no-op on empty)
    if chunks_tbl is not None:
        with StageTimer("ingest.db.ensure_vector_index", extra={"metric": "cosine"}):
            ensure_vector_index(chunks_tbl, column="vector", metric="cosine")

    # Pre-warm cache (best-effort) so second run can skip embeds.
    if vcache_tbl is None:
        try:
            probe_dim = int(emb.get("dim", dim or 768))
            vcache_tbl = ensure_vec_cache(db, probe_dim)
            vec_cache = load_vec_cache_map(vcache_tbl, model)
            if "dim" in emb and chunks_tbl is None:
                chunks_tbl = ensure_chunks(db, table_name, int(emb["dim"]))
        except Exception:
            vec_cache = {}
            vcache_tbl = None

     # close counters
    file_bar.close()
    chunk_bar.close()
    embed_bar.close()
    write_bar.close()
    rprint("[green]Ingest complete.[/green]")

def _flush_rows(
    pending_rows: List[dict],
    chunks_tbl,
    vcache_tbl,
    model: str,
    dim: Optional[int],
) -> None:
    if not pending_rows:
        return
    print(f"DEBUG: flush_rows with len(rows)={len(pending_rows)}, expected dim={dim}", file=sys.stderr)
    if pending_rows:
        example_vec = pending_rows[0].get("vector")
        print(f"DEBUG: example row vector length={len(example_vec) if example_vec else None}", file=sys.stderr)
    if chunks_tbl is None or dim is None:
        # nothing to write yet (waiting to learn dim)
        return
    rows = validate_vectors(pending_rows, dim, key="vector")
    upsert_rows(chunks_tbl, rows, on=["id"])

    # NEW: persist cache entries
    if vcache_tbl is not None:
        cache_rows = [
            {"chunk_sha": r["content_sha"], "model": model, "vector": r["vector"]}
            for r in rows
            if r.get("vector") is not None
        ]
        if cache_rows:
            upsert_rows(vcache_tbl, cache_rows, on=["chunk_sha", "model"])

    pending_rows.clear()