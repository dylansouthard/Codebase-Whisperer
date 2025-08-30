
from typing import List
import re

def split_by_size(text: str, max_chars: int) -> List[str]:
    """
    Split text into chunks no longer than max_chars.
    Prefers splitting at whitespace/newlines but will hard-split
    if a single token exceeds max_chars (e.g., minified JS).
    """
    if len(text) <= max_chars:
        return [text]
    # split on safe boundaries
    parts = re.split(r'(\n|\s)', text)
    out, buf = [], ""
    for p in parts:
        if len(buf) + len(p) > max_chars:
            out.append(buf)
            buf = ""

            while len(p) > max_chars:
                out.append(p[:max_chars])
                p = p[max_chars:]
        buf += p
    if buf:
        out.append(buf)
    return out
