



from typing import List, Optional, Tuple
import os
import re
from .common import split_by_size



JAVA_METHOD_RE = re.compile(
    r'(?P<sig>(public|private|protected|\s)*\s*(static|\s)*\s*[\w\<\>\[\]]+\s+\w+\s*\([^\)]*\)\s*(throws\s+[\w\.,\s]+)?\s*)\{',
    re.MULTILINE
)

def chunk_java(text: str, max_chars: int) -> List[Tuple[str, Optional[str]]]:
    """
    Returns list of (chunk_text, symbol_name).
    Starts with class header + methods; falls back to line-based splitting.
    """
    chunks: List[Tuple[str, Optional[str]]] = []

    # coarse split by class blocks to keep context
    classes = re.split(r'(?=^\s*(public|abstract|final)?\s*class\s+\w+)|(?=^\s*interface\s+\w+)|(?=^\s*enum\s+\w+)', text, flags=re.MULTILINE)
    for block in classes:
        block = block.strip()
        if not block: continue
        # within a class/interface, split by method signatures
        last = 0
        for m in JAVA_METHOD_RE.finditer(block):
            start = m.start()
            seg = block[last:start].strip()
            if seg:
                # class-level stuff or previous method body remainder
                for piece in split_by_size(seg, max_chars):
                    chunks.append((piece, None))
            # include method body heuristically by matching braces
            method_start = start
            brace_depth = 0
            i = block.find("{", method_start)
            if i == -1:
                last = start
                continue
            brace_depth = 1
            j = i + 1
            while j < len(block) and brace_depth > 0:
                if block[j] == "{": brace_depth += 1
                elif block[j] == "}": brace_depth -= 1
                j += 1
            method_text = block[method_start:j]
            sig = m.group("sig").strip()
            symbol = _symbol_from_signature(sig)
            for piece in split_by_size(method_text, max_chars):
                chunks.append((piece, symbol))
            last = j
        # tail
        tail = block[last:].strip()
        if tail:
            for piece in split_by_size(tail, max_chars):
                chunks.append((piece, None))

    # fallback if somehow nothing
    if not chunks:
        for piece in split_by_size(text, max_chars):
            chunks.append((piece, None))
    return chunks

def _symbol_from_signature(sig: str) -> str:
    # naive extract method name()
    m = re.search(r'\b([A-Za-z_]\w*)\s*\(', sig)
    return m.group(1) if m else "unknown"

