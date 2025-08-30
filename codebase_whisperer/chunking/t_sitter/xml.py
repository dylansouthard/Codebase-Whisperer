from __future__ import annotations
from typing import Optional
from .util import node_text, get_children


def xml_start_tag(node):
    for ch in get_children(node):
        if ch.type == "start_tag":
            return ch
    return None

def xml_tag_name(utf8: bytes, element_node) -> Optional[str]:
    st = xml_start_tag(element_node)
    if not st: return None
    for t in get_children(st):
        if t.type == "tag_name":
            return node_text(utf8, t).strip()
    return None

def xml_attr_value(utf8: bytes, start_tag_node, wanted: str) -> Optional[str]:
    """
    Return the value of attribute `wanted` from a start_tag.
    Handles attribute value node types across XML grammars.
    """
    VALUE_TYPES = {"attribute_value", "quoted_attribute_value", "string"}

    for ch in get_children(start_tag_node):
        if ch.type != "attribute":
            continue

        name = None
        val = None

        # 1) First pass: look at direct children
        for a in get_children(ch):
            if a.type == "attribute_name":
                name = node_text(utf8, a).strip()
            elif a.type in VALUE_TYPES:
                raw = node_text(utf8, a).strip()
                # strip quotes if present
                if len(raw) >= 2 and raw[0] == raw[-1] and raw[0] in {'"', "'"}:
                    raw = raw[1:-1]
                val = raw

        # 2) If value not found, do a cheap descendant scan (some grammars nest)
        if name == wanted and val is None:
            stack = list(get_children(ch))
            while stack:
                n = stack.pop()
                if n.type in VALUE_TYPES:
                    raw = node_text(utf8, n).strip()
                    if len(raw) >= 2 and raw[0] == raw[-1] and raw[0] in {'"', "'"}:
                        raw = raw[1:-1]
                    val = raw
                    break
                stack.extend(get_children(n))

        if name == wanted:
            return val

    return None

def xml_fallback_tag_name(utf8: bytes, node) -> Optional[str]:
    """
    Fallback when xml_tag_name(...) can’t find a tag due to grammar differences.
    We heuristically parse the start tag text: e.g. '<mapper namespace="...">' -> 'mapper'.
    """
    st = xml_start_tag(node) or node
    txt = node_text(utf8, st).lstrip()
    if not txt.startswith("<"):
        return None
    # Pull the token right after '<', stopping at space, '>', or '/>'
    head = txt[1:]
    for stop in (" ", "\t", "\n", ">", "/"):
        idx = head.find(stop)
        if idx != -1:
            head = head[:idx]
            break
    head = head.strip()
    return head or None
