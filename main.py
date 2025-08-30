#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Local-only Codebase Expert (Java/Spring-first)
- Index: walk repo -> chunk -> embed (Ollama) -> store (LanceDB)
- Query: embed question -> vector search -> build answer with LLM (Ollama)
"""
import os
import argparse
import fnmatch
import hashlib

import sys
from typing import Any, Dict, Iterable, List, Optional, Tuple

import lancedb
from pydantic import BaseModel
from rich import print as rprint
from rich.panel import Panel
from tqdm import tqdm

from codebase_whisperer.ollama import OllamaClient
from codebase_whisperer.config import load_config

os.environ.setdefault("UVLOOP_NO_WARN_ERROR", "1")
try:
    import uvloop
    uvloop.install()
except ImportError:
    pass



# ---------------------------
# Utilities
# ---------------------------



def read_text(path: str) -> str:
    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()
    except Exception as e:
        rprint(f"[yellow]Failed to read {path}: {e}[/yellow]")
        return ""

def human_bytes(n: int) -> str:
    for unit in ["B","KB","MB","GB"]:
        if n < 1024.0: return f"{n:.1f}{unit}"
        n /= 1024.0
    return f"{n:.1f}TB"





# ---------------------------
# DB schema
# ---------------------------

class ChunkRow(BaseModel):
    id: str
    path: str
    relpath: str
    lang: str
    symbol: Optional[str]
    chunk_idx: int
    content: str
    sha256: str
    mtime: float
    vector: List[float]

# ---------------------------
# Indexer
# ---------------------------


def ensure_table(db: lancedb.db.DBConnection, table_name: str):
    if table_name in db.table_names():
        return db.open_table(table_name)
    schema = {
        "id": lancedb.VARCHAR(64),
        "path": lancedb.VARCHAR(2048),
        "relpath": lancedb.VARCHAR(2048),
        "lang": lancedb.VARCHAR(32),
        "symbol": lancedb.VARCHAR(256),
        "chunk_idx": lancedb.INT32(),
        "content": lancedb.TEXT(),
        "sha256": lancedb.VARCHAR(64),
        "mtime": lancedb.FLOAT64(),
        "vector": lancedb.Vector(768),  # nomic-embed-text dim
    }
    tbl = db.create_table(table_name, schema=schema)
    tbl.create_index(vector_column="vector", metric="cosine")
    return tbl

def index_repo(cfg: Dict[str, Any], repo_root: str, only_changed: bool=False):
    ollama = OllamaClient(cfg["ollama"]["host"])
    db = lancedb.connect(os.path.join(repo_root, cfg["db_dir"]))
    tbl = ensure_table(db, cfg["table"])

    # Build path -> existing sha map for incremental index
    existing = {}
    if cfg["table"] in db.table_names():
        for batch in tbl.to_pandas_batches(columns=["id","path","sha256"]):
            for _, row in batch.iterrows():
                existing[(row["id"], row["path"])] = row["sha256"]

    includes = cfg["indexing"]["include_globs"]
    excludes = cfg["indexing"]["exclude_globs"]
    max_mb = cfg["indexing"]["max_file_mb"]
    max_chunk = cfg["indexing"]["max_chunk_chars"]
    min_chunk = cfg["indexing"]["min_chunk_chars"]

    java_first = True
    use_ts = cfg["indexing"].get("use_tree_sitter_java", True)
    parser, lang = (None, None)
    if use_ts:
        parser, lang = try_tree_sitter_java()

    files = list(iter_files(repo_root, includes, excludes, max_mb))
    rprint(Panel.fit(f"Indexing {len(files)} files from [bold]{repo_root}[/bold]"))
    to_upsert = []

    for fpath in tqdm(files, desc="Reading"):
        rel = os.path.relpath(fpath, repo_root)
        text = read_text(fpath)
        if not text.strip():
            continue
        sha = file_sha256(fpath)
        mtime = os.path.getmtime(fpath)
        lang_guess = approx_language(fpath)

        if only_changed:
            # quick skip if we already have these exact chunks (sha-based id)
            # (we still need to compute new chunk ids if file changed)
            # so only skip if exact sha exists for this path
            already = any(v == sha for (cid, p), v in existing.items() if p == fpath)
            if already:
                continue

        # chunk
        chunks_with_symbol: List[Tuple[str, Optional[str]]] = []
        if lang_guess == "java":
            # try method-aware chunking
            chunks_with_symbol = chunk_java(text, max_chunk)
        else:
            for c in chunk_plain(text, max_chunk, min_chunk):
                chunks_with_symbol.append((c, None))

        # embed in small batches (avoid huge payloads)
        batch_texts = [c for c, _ in chunks_with_symbol]
        vectors = ollama.embed(cfg["ollama"]["embed_model"], batch_texts)

        for idx, ((content, symbol), vec) in enumerate(zip(chunks_with_symbol, vectors)):
            cid = sha256_bytes((f"{fpath}:{idx}:{sha}").encode("utf-8"))
            to_upsert.append({
                "id": cid,
                "path": fpath,
                "relpath": rel,
                "lang": lang_guess,
                "symbol": symbol or "",
                "chunk_idx": idx,
                "content": content,
                "sha256": sha,
                "mtime": float(mtime),
                "vector": vec,
            })

        # flush by chunks to keep memory steady
        if len(to_upsert) >= 2000:
            tbl.merge_insert(to_upsert, on="id")
            to_upsert.clear()

    if to_upsert:
        tbl.merge_insert(to_upsert, on="id")

    rprint("[green]Indexing complete.[/green]")

# ---------------------------
# Retrieval + Answering
# ---------------------------

SYSTEM_PROMPT = """You are Codebase Expert: a senior engineer assistant.
- You answer based on provided code snippets only; if unsure, say so.
- Prefer precise, actionable explanations and minimal fluff.
- If the user asks for locations/paths, list file paths and method names.
- If a fix is obvious, propose a minimal diff.
"""

def search_chunks(cfg: Dict[str, Any], repo_root: str, query: str, top_k: int):
    ollama = OllamaClient(cfg["ollama"]["host"])
    qvec = ollama.embed(cfg["ollama"]["embed_model"], [query])[0]
    db = lancedb.connect(os.path.join(repo_root, cfg["db_dir"]))
    tbl = db.open_table(cfg["table"])
    hits = tbl.search(qvec).metric("cosine").limit(top_k).to_pandas()
    # ensure we have content, relpaths
    results = []
    for _, row in hits.iterrows():
        results.append({
            "id": row["id"],
            "path": row["path"],
            "relpath": row["relpath"],
            "lang": row.get("lang", ""),
            "symbol": row.get("symbol", ""),
            "chunk_idx": int(row.get("chunk_idx", 0)),
            "content": row["content"],
            "score": float(row["_distance"]) if "_distance" in row else 0.0,
        })
    return results

def build_context(hits: List[Dict[str, Any]], max_chars: int) -> str:
    buf = []
    total = 0
    for h in hits:
        header = f"=== {h['relpath']}  #{h['chunk_idx']}  {('::'+h['symbol']) if h['symbol'] else ''}".rstrip() + "\n"
        body = h["content"].rstrip() + "\n"
        piece = header + body
        if total + len(piece) > max_chars:
            break
        buf.append(piece)
        total += len(piece)
    return "\n".join(buf)

def answer(cfg: Dict[str, Any], repo_root: str, question: str, hits: List[Dict[str, Any]]) -> str:
    ctx = build_context(hits, cfg["retrieval"]["max_context_chars"])
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": f"Question:\n{question}\n\nContext from repository (do not reveal beyond this):\n{ctx}\n\nInstructions:\n- Cite files and methods when referring to code.\n- If you propose a change, output a minimal unified diff when possible.\n"}
    ]
    ollama = OllamaClient(cfg["ollama"]["host"])
    return ollama.chat(cfg["ollama"]["chat_model"], messages, stream=True)

# ---------------------------
# CLI
# ---------------------------

def main():
    ap = argparse.ArgumentParser(description="Local Codebase Expert (Java/Spring-first)")
    ap.add_argument("--config", type=str, help="Path to YAML config")
    sub = ap.add_subparsers(dest="cmd", required=True)

    ap_idx = sub.add_parser("index", help="Index (full)")
    ap_idx.add_argument("--repo", type=str, default=None, help="Repo root (default: from config)")
    ap_idx.add_argument("--changed-only", action="store_true", help="Only index files whose sha changed")

    ap_q = sub.add_parser("query", help="Ask a question")
    ap_q.add_argument("--repo", type=str, default=None)
    ap_q.add_argument("question", type=str, nargs="+", help="Question text")

    args = ap.parse_args()
    cfg = load_config(args.config)
    repo_root = os.path.abspath(args.repo or cfg["repo_root"])

    if args.cmd == "index":
        os.makedirs(os.path.join(repo_root, cfg["db_dir"]), exist_ok=True)
        index_repo(cfg, repo_root, only_changed=args.changed_only)
    elif args.cmd == "query":
        q = " ".join(args.question)
        hits = search_chunks(cfg, repo_root, q, cfg["retrieval"]["top_k"])
        if not hits:
            rprint("[yellow]No results found. Did you run 'index' first?[/yellow]")
            sys.exit(1)
        rprint(Panel.fit(f"Top-{len(hits)} retrieved chunks"))
        for h in hits[:5]:
            rprint(f"[cyan]{h['relpath']}[/cyan]  score={h['score']:.4f}  symbol={h['symbol'] or '-'}")
        ans = answer(cfg, repo_root, q, hits)
        print("\n" + ans)

if __name__ == "__main__":
    main()
