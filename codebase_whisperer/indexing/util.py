import hashlib
import os
from typing import Optional, Dict, Iterable, List
from pathlib import Path
from rich import print as rprint
from .languages import FILENAME_LANGUAGE_MAP, EXT_LANGUAGE_MAP

def sha256_bytes(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()

def file_sha256(path: str) -> str:
    with open(path, "rb") as f:
        return sha256_bytes(f.read())

def approx_language(
        path: str | os.PathLike[str],
        *,
        filename_map: Optional[Dict[str, str]] = None,
        ext_map:Optional[Dict[str, str]] = None,
        default: str = "text"
                    ) -> str:
    filename_map = filename_map or FILENAME_LANGUAGE_MAP
    ext_map = ext_map or EXT_LANGUAGE_MAP

    p = Path(path)
    name = p.name.lower()

    if name in filename_map:
        return filename_map[name]

    suffixes = [s.lower() for s in p.suffixes]
    if len(suffixes) >= 2:
        combo = "".join(suffixes[-2:])
        if combo in ext_map:
            return ext_map[combo]

    if p.suffix:
        lang = ext_map.get(p.suffix.lower())
        if lang:
            return lang

    return default

def read_text(
        path: str | os.PathLike[str],
        *,
        encodings: Optional[Iterable[str]] = None,
        ) -> str:
    """
    Try multiple encodings until one works. The list of encodings should
    come from config.py (indexing.encodings).
    """

    candidates: List[str] = list(encodings) if encodings else ["utf-8"]
    num_tries = 0
    for enc in candidates:
        try:
            with open(path, "r", encoding=enc, errors="strict") as f:
                return f.read()
        except Exception as e:
            num_tries += 1
            if num_tries == len(candidates) - 1:
                rprint(f"[yellow]Failed to read {path}: {e}[/yellow]")
            continue
        return ""
