from __future__ import annotations
from typing import Dict, List, Optional, Tuple, Any

from ..common import split_by_size
from .util import node_text, child_text_by_type

from .lang_nodes import LANG_NODE_MAP
from .walkers import walk_code, walk_markdown, walk_xml, normalize_sym



def _lang_key(parser, hint: Optional[str]) -> str:
    try:
        return (getattr(parser, "language", None) and parser.language.name) or (hint or "")
    except Exception:
        return hint or ""

def extract_defs(
        lang_name: str,
        parser,
        text: str,
        lang_node_map: Optional[Dict[str, Any]] = None,
) -> List[Tuple[str, str]]:
    if not text or not text.strip():
        return []

    spec_map = lang_node_map or LANG_NODE_MAP
    utf8 = text.encode("utf-8")
    tree = parser.parse(utf8)
    root = tree.root_node

    key = _lang_key(parser, lang_name)
    key_short = (key or "").split(".")[0]
    spec = (
        spec_map.get(key) or
        spec_map.get(key_short) or
        spec_map.get(lang_name) or
        spec_map.get(lang_name.split(".")[0], None)
    )

    if spec is None:
        return [("", text)]

    extra: Dict[str, Any] = spec.get("extra", {})

    defs: List[Tuple[str, str]] = []

    def emit(symbol: Any, node):
        sym_s = normalize_sym(symbol)
        code = node_text(utf8, node).strip()
        if code:
            defs.append((sym_s, code))

    if key_short in {"java","kotlin","groovy","typescript","tsx","javascript","python"}:
        walk_code(root, spec=spec, utf8=utf8, emit=emit, container_sym=None)
    elif key_short == "markdown":
        walk_markdown(root, utf8=utf8, extra=extra, emit=emit)
    elif key_short == "xml":
        walk_xml(root, utf8=utf8, extra=extra, emit=emit, ns=None)
    else:
        for ch in getattr(root, "children", []):
            emit("", ch)

    out: List[Tuple[str, str]] = []
    seen: set[Tuple[str, str]] = set()
    for sym, code in defs:
        sym_s = normalize_sym(sym)
        k = (sym_s, code[:120])
        if k in seen:
            continue
        seen.add(k)
        out.append((sym_s, code))
    return out


def chunk_defs_with_limits(defs: List[Tuple[str, str]], max_chars: int) -> List[Tuple[str, Optional[str]]]:
    out: List[Tuple[str, Optional[str]]] = []
    for sym, snippet in defs:
        for piece in split_by_size(snippet, max_chars):
            out.append((piece, sym or None))
    return out