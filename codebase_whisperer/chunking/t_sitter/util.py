from __future__ import annotations
from typing import List

def get_children(node):
    return getattr(node, "children", [])

def node_text(utf8: bytes, node) -> str:
    return utf8[node.start_byte:node.end_byte].decode("utf-8", "replace")

def child_text(utf8: bytes, node, type_name) -> str:
    for ch in get_children(node):
        if ch.type == type_name:
            return node_text(utf8, ch).strip()
    return None

def child_text_by_type(utf8: bytes, node, type_name: str) -> List[str]:
    out: List[str] = []
    for ch in get_children(node):
        if ch.type == type_name:
            out.append(node_text(utf8, ch).strip())
    return out

def heading_text(utf8: bytes, node) -> str:
    return node_text(utf8, node).lstrip("#").strip()
