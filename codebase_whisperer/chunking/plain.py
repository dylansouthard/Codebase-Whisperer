

import re
from typing import List
from .common import split_by_size

def chunk_plain(text: str, max_chars: int, min_chars: int) -> List[str]:
    # split by blank lines, then merge up to max_chars
    paras = [p.strip() for p in re.split(r'\n\s*\n', text) if p.strip()]
    chunks = []
    buf = ""
    for p in paras:
        if len(buf) + len(p) + 2 <= max_chars:
            buf = (buf + "\n\n" + p) if buf else p
        else:
            if buf:
                chunks.append(buf)
            if len(p) <= max_chars:
                buf = p
            else:
                chunks.extend(split_by_size(p, max_chars))
                buf = ""
    if buf:
        chunks.append(buf)
    # ensure min size by merging small tails
    merged = []
    cur = ""
    for c in chunks:
        if len(cur) < min_chars:
            cur = (cur + "\n\n" + c) if cur else c
        else:
            merged.append(cur)
            cur = c
    if cur:
        merged.append(cur)
    return merged
