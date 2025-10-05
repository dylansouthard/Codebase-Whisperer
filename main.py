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

from codebase_whisperer.llm.ollama import OllamaClient
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
