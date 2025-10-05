# codebase_whisperer/indexing/indexer.py
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Iterator, Optional

from rich import print as rprint

from codebase_whisperer.config import load_config
from codebase_whisperer.indexing.languages import (
    FILENAME_LANGUAGE_MAP,
    EXT_LANGUAGE_MAP,
)
from codebase_whisperer.indexing.util import (
    approx_language,
    read_text,
    file_sha256,
    sha256_bytes,
)
from .walker import iter_files  # use your walker; do NOT re-implement traversal

# -------------------------
# debug helpers (opt-in)
# -------------------------
def _dbg_on() -> bool:
    return os.getenv("DEBUG_INDEXER", "").lower() in {"1", "true", "t", "yes", "y"}

def _log(*args):
    if _dbg_on():
        rprint("[indexer]", *args)

# -------------------------
# public data model
# -------------------------
@dataclass(frozen=True)
class FileRecord:
    # core
    path: str              # absolute path (as seen during walk)
    relpath: str           # relative to repo root
    realpath: str          # resolved path if symlink, else same as path
    is_symlink: bool
    size_bytes: int
    mtime: float

    # content / ids
    sha256: str            # hash of REAL file bytes
    content_sha: str       # sha256 of decoded text content
    lang: str              # normalized language name
    content: str           # decoded text

    # --- Public API compatibility (properties) ---
    @property
    def rel_path(self) -> str:
        return self.relpath

    @property
    def real_path(self) -> str:
        return self.realpath

    @property
    def language(self) -> str:
        return self.lang

    @property
    def text(self) -> str:
        return self.content

# -------------------------
# public api
# -------------------------
def index_repo(
    repo_root: str | os.PathLike[str],
    *,
    config_path: Optional[str] = None,
    include_globs: Optional[Iterable[str]] = None,
    exclude_globs: Optional[Iterable[str]] = None,
    encodings: Optional[Iterable[str]] = None,
    max_file_mb: Optional[float] = None,
    follow_symlinks: Optional[bool] = None,
    include_hidden: Optional[bool] = None,
) -> Iterator[FileRecord]:
    """
    High-level generator:
      - loads indexing config
      - walks files using your walker
      - classifies language
      - reads content via configured encodings
      - yields FileRecord per file

    Optional kwargs override values from config for testing/CLI convenience.
    """
    cfg, cfg_file = load_config(config_path)
    idx_cfg = cfg.get("indexing", {}) if isinstance(cfg, dict) else {}

    # merge: kwargs override config
    final_include = list(include_globs) if include_globs is not None else list(idx_cfg.get("include_globs", []))
    final_exclude = list(exclude_globs) if exclude_globs is not None else list(idx_cfg.get("exclude_globs", []))
    final_encodings = list(encodings) if encodings is not None else list(idx_cfg.get("encodings", ["utf-8"]))
    final_max_mb = float(max_file_mb) if max_file_mb is not None else float(idx_cfg.get("max_file_mb", 1.5))
    final_follow = bool(follow_symlinks) if follow_symlinks is not None else bool(idx_cfg.get("follow_symlinks", False))
    final_hidden = bool(include_hidden) if include_hidden is not None else bool(idx_cfg.get("include_hidden", True))

    root = Path(repo_root).resolve()
    _log(f"repo_root={root}")
    if cfg_file:
        _log(f"loaded config from {cfg_file}")
    _log(
        f"include={final_include} exclude={final_exclude} encodings={final_encodings} "
        f"max_file_mb={final_max_mb} follow_symlinks={final_follow} include_hidden={final_hidden}"
    )

    max_bytes = int(final_max_mb * 1024 * 1024)

    for path in iter_files(
        root=str(root),
        include_globs=final_include,
        exclude_globs=final_exclude,
        follow_symlinks=final_follow,
        include_hidden=final_hidden,
    ):
        try:
            rec = index_file(
                path=path,
                repo_root=root,
                encodings=final_encodings,
                max_bytes=max_bytes,
            )
            if rec is None:
                continue
            yield rec
            _log(f"[ok] {rec.relpath} ({rec.lang}, {rec.size_bytes} bytes)")
        except Exception as e:
            rprint(f"[yellow][indexer] failed {path}: {e}[/yellow]")

def index_file(
    *,
    path: str | os.PathLike[str],
    repo_root: str | os.PathLike[str],
    encodings: Iterable[str],
    max_bytes: int,
) -> Optional[FileRecord]:
    """
    Index a single file path into a FileRecord or None (if skipped).
    Skips files larger than max_bytes.
    """
    p = Path(path)
    real = Path(os.path.realpath(p))
    try:
        st = real.stat()
    except FileNotFoundError:
        return None  # broken symlink or race

    size = int(getattr(st, "st_size", 0))
    if size > max_bytes:
        _log(f"[skip too large] {p} ({size} bytes > {max_bytes})")
        return None

    # decode text (uses your util.read_text)
    text = read_text(str(real), encodings=encodings)

    # language classification (uses your language maps)
    lang = approx_language(
        str(real),
        filename_map=FILENAME_LANGUAGE_MAP,
        ext_map=EXT_LANGUAGE_MAP,
        default="text",
    )

    # hashes + paths
    file_hash = file_sha256(str(real))
    content_hash = sha256_bytes(text.encode("utf-8", "replace"))

    root_abs = Path(repo_root).absolute()
    p_abs = Path(path).absolute()  # absolute() does NOT resolve symlinks
    rel = p_abs.relative_to(root_abs).as_posix()

    return FileRecord(
        path=str(Path(path).resolve()),
        realpath=str(real),
        is_symlink=Path(path).is_symlink(),
        relpath=rel,
        lang=lang,
        sha256=file_hash,
        content_sha=content_hash,
        mtime=float(getattr(st, "st_mtime", 0.0)),
        size_bytes=size,
        content=text,
    )