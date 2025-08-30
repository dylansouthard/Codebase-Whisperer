# codebase_whisperer/indexing/walker.py
from __future__ import annotations

import os
import fnmatch
from pathlib import Path
from typing import Iterable, Iterator, List, Optional, Tuple
from rich import print as rprint


def _to_posix(p: str) -> str:
    """Normalize path separators to '/' for consistent glob matching on all OSes."""
    return p.replace(os.sep, "/") if os.sep != "/" else p


def _split_globs(globs: Iterable[str]) -> Tuple[List[str], List[str]]:
    """
    Split patterns into:
      - dir_globs: patterns intended to match whole directories (ending with '/**')
      - file_globs: everything else (file patterns or bare filenames/paths)
    We store dir_globs *without* the trailing '/**' for easier matching.
    """
    dir_globs: List[str] = []
    file_globs: List[str] = []
    for g in globs:
        g_posix = _to_posix(g)
        if g_posix.endswith("/**"):
            dir_globs.append(g_posix[:-3])  # drop '/**'
        else:
            file_globs.append(g_posix)
    return dir_globs, file_globs


def _fnmatch_with_double_star(rel_posix: str, pattern: str) -> bool:
    """
    fnmatch doesn't treat '**' specially; '**/*.txt' won't match 'a.txt' at root.
    Workaround: if pattern starts with '**/', also try it without that prefix.
    """
    if fnmatch.fnmatch(rel_posix, pattern):
        return True
    if pattern.startswith("**/"):
        return fnmatch.fnmatch(rel_posix, pattern[3:])
    return False


def _matches_any(rel_posix: str, patterns: List[str]) -> bool:
    return any(_fnmatch_with_double_star(rel_posix, pat) for pat in patterns)


def _basename_from_dir_glob(g: str) -> str:
    """
    From a dir glob like '**/target' or 'src/main', return the last segment ('target', 'main').
    Used for fast subtree pruning by directory name.
    """
    g = g.strip("/ ")
    if not g:
        return g
    return g.split("/")[-1]


def _path_contains_segment(rel_posix: str, segment: str) -> bool:
    """
    True if rel_posix contains '/segment/' as a directory component (or equals segment).
    Ex: rel='target/gen/Bar.java', segment='target' -> True
    """
    s = "/" + rel_posix.strip("/") + "/"
    seg = "/" + segment.strip("/") + "/"
    return seg in s or rel_posix == segment


def iter_files(
    root: str | os.PathLike[str],
    include_globs: Iterable[str],
    exclude_globs: Iterable[str],
    *,
    max_file_mb: Optional[float] = None,
    follow_symlinks: bool = False,
    include_hidden: bool = True,
) -> Iterator[str]:
    """
    Walk `root` and yield ABSOLUTE file paths that:
      - match at least one include_glob (if include_globs non-empty)
      - do NOT match any exclude_glob
      - are <= max_file_mb (if provided)

    Globs are interpreted relative to `root`, using POSIX '/' separators, e.g.:
      "**/*.java", "**/target/**", "src/main/**", "pom.xml"

    Exclude globs ending with '/**' are used to prune entire subtrees early.
    """
    root_path = Path(root).resolve()
    if not root_path.is_dir():
        return

    ex_dir_globs, ex_file_globs = _split_globs(exclude_globs)
    in_dir_globs, in_file_globs = _split_globs(include_globs)
    max_bytes = int(max_file_mb * 1024 * 1024) if max_file_mb else None

    # For pruning, compare by basename (target, .git, etc.)
    pruned_name_set = { _basename_from_dir_glob(g) for g in ex_dir_globs if _basename_from_dir_glob(g) }

    for dirpath, dirnames, filenames in os.walk(root_path, followlinks=follow_symlinks):
        # Compute rel dir ('' for root)
        rel_dir = _to_posix(os.path.relpath(dirpath, root_path))
        if rel_dir == ".":
            rel_dir = ""

        # Optionally hide dot-directories fast
        if not include_hidden:
            dirnames[:] = [d for d in dirnames if not d.startswith(".")]

        # Directory-level pruning using exclude dir globs (by basename)
        if pruned_name_set:
            dirnames[:] = [d for d in dirnames if d not in pruned_name_set]

        # Handle files in this directory
        for fn in filenames:
            # Optionally hide dotfiles
            if not include_hidden and fn.startswith("."):
                continue

            full = Path(dirpath) / fn
            try:
                # Skip symlinked files unless following
                if full.is_symlink() and not follow_symlinks:
                    continue
                # Size gating
                if max_bytes is not None and full.stat().st_size > max_bytes:
                    continue
            except OSError as e:
                rprint(f"[yellow]Skipping {full}: {e}[/yellow]")
                continue

            # Build rel path for glob matching
            rel = _to_posix(os.path.relpath(full, root_path))

            # Exclude checks
            if ex_file_globs and _matches_any(rel, ex_file_globs):
                continue
            # Also guard against files within excluded dirs (by basename)
            if pruned_name_set and any(_path_contains_segment(rel, seg) for seg in pruned_name_set):
                continue

            # Include checks
            include_ok = True
            if include_globs:
                include_ok = _matches_any(rel, in_file_globs) or (
                    # allow directory includes like "src/main/**"
                    in_dir_globs and any(
                        rel == g or rel.startswith(g.rstrip("/") + "/")
                        for g in in_dir_globs
                    )
                )
            if not include_ok:
                continue

            # Preserve symlink path when following so callers can see link names.
            if follow_symlinks and full.is_symlink():
                yield str(full)
            else:
                yield str(full.resolve())