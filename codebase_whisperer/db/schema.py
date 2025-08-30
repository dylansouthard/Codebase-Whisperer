# codebase_whisperer/db/schema.py
from __future__ import annotations
import pyarrow as pa

def chunks_schema(embedding_dim: int) -> pa.schema:
    return pa.schema([
        ("id", pa.string()),          # unique per (path, chunk)
        ("path", pa.string()),        # user-facing path (may be symlink)
        ("realpath", pa.string()),    # resolved path (target)
        ("is_symlink", pa.bool_()),
        ("relpath", pa.string()),     # path relative to repo root
        ("lang", pa.string()),
        ("symbol", pa.string()),      # method/function name if available
        ("chunk_idx", pa.int32()),
        ("content", pa.string()),     # text content
        ("sha256", pa.string()),      # file-byte hash of REAL file
        ("content_sha", pa.string()), # sha256(text content)
        ("mtime", pa.float64()),
         ("vector", pa.list_(pa.float32())),  # embedding vector,  # embedding vector
    ])

def vec_cache_schema(embedding_dim: int) -> pa.schema:
    return pa.schema([
        ("chunk_sha", pa.string()),   # content_sha
        ("model", pa.string()),
        ("vector", pa.list_(pa.float32())),
    ])