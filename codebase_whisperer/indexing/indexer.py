
import os
import sys
import fnmatch
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, Iterator, List, Optional, Tuple

from rich import print as rprint

# --- project imports ---
from codebase_whisperer.config import load_config, DEFAULT_CONFIG
from codebase_whisperer.indexing.languages import FILENAME_LANGUAGE_MAP, EXT_LANGUAGE_MAP
from codebase_whisperer.indexing.util import (
    sha256_bytes,
    file_sha256,
    approx_language,
    read_text,
)
from codebase_whisperer.chunking import (
    get_ts_parser,
    extract_defs,
    chunk_defs_with_limits,
)
from codebase_whisperer.chunking.plain import chunk_plain

# -------------------------
# Debug helpers (opt-in via env)
# -------------------------
def _dbg_on() -> bool:
    return os.getenv("DEBUG_INDEXER", "").lower() in {"1", "true", "t", "yes", "y"}

def _log(*args, **kwargs):
    if _dbg_on():
        rprint("[indexer]", *args, **kwargs)


# -------------------------
# Data classes
# -------------------------
@dataclass(frozen=True)
class FileDoc:
    path: str                 # absolute path
    rel_path: str             # path relative to repo_root
    sha256: str               # file content hash
    language: str             # approx language ("java", "xml", "markdown", "text", ...)
    size_bytes: int

@dataclass(frozen=True)
class ChunkDoc:
    file_sha256: str
    file_rel_path: str
    language: str
    symbol: Optional[str]     # for code chunks: class/method/etc; for plain text: None
    text: str                 # chunk content
    idx: int                  # chunk index in this file (0..N-1)
    total: int                # total chunks for this file
# -------------------------
# Small helper to read "indexing" values with a DEFAULT_CONFIG safety net
# -------------------------
def _idx(cfg: Dict[str, Any], key: str):
    """
    Read cfg['indexing'][key] with fallback to DEFAULT_CONFIG['indexing'][key].
    Does not duplicate literal defaults in this file.
    """
    idx = cfg.get("indexing") or {}
    if key in idx:
        return idx[key]
    return DEFAULT_CONFIG["indexing"][key]

def build_index(
        repo_root: str | os.PathLike[str] = "./",
        config_path:Optional[str] = None
) -> List[ChunkDoc]:
    """
    High-level entry: load config, walk files, chunk them, return ChunkDoc list.

    Pulls everything from config.py via load_config(config_path).
    Falls back per-key to DEFAULT_CONFIG if a key is missing (safety net).

    Uses:
      indexing.include_globs   (List[str])
      indexing.exclude_globs   (List[str])
      indexing.encodings       (List[str])
      indexing.max_file_mb     (float)
      indexing.max_chunk_chars (int)
      indexing.min_chunk_chars (int)
    """
    cfg, cfg_file = load_config(config_path)

    repo_root = str(Path(repo_root).resolve())

    include_globs: List[str] = list(_idx(cfg, "include_globs"))
    exclude_globs: List[str] = list(_idx(cfg, "exclude_globs"))
    encodings: List[str] = list(_idx(cfg, "encodings"))
    max_file_mb: float = float(_idx(cfg, "max_file_mb"))
    max_chunk_chars: int = int(_idx(cfg, "max_chunk_chars"))
    min_chunk_chars: int = int(_idx(cfg, "min_chunk_chars"))


    _log(f"repo_root={repo_root}")
    if cfg_file:
        _log(f"loaded config from: {cfg_file}")
    _log(f"include_globs={include_globs}")
    _log(f"exclude_globs={exclude_globs}")
    _log(f"encodings={encodings}")
    _log(f"max_file_mb={max_file_mb}, max_chunk_chars={max_chunk_chars}, min_chunk_chars={min_chunk_chars}")

    files = list(
        iter_files(
            repo_root=repo_root,
            include_globs=include_globs,
            exclude_globs=exclude_globs
        )
    )

    _log(f"candidate files={len(files)}")

    out_chunks: List[ChunkDoc] = []
    ts_parser_cache: Dict[str, Any] = {}

    for path in files:
        try:
            file_info = describe_file(path, repo_root)
            chunks = chunk_file(
                file_info=file_info,
                encodings=encodings,
                max_chunk_chars=max_chunk_chars,
                min_chunk_chars=min_chunk_chars,
                ts_parser_cache=ts_parser_cache,
            )
            out_chunks.extend(chunks)
            _log(f"[ok] {file_info.rel_path}: {len(chunks)} chunks")
        except Exception as e:
            rprint(f"[yellow][indexer] Failed to index {path}: {e}[/yellow]")

    _log(f"[yellow][indexer] Failed to index {path}: {e}[/yellow]")
    return out_chunks




