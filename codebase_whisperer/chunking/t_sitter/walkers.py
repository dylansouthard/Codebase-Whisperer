# codebase_whisperer/chunking/t_sitter/walkers.py
from __future__ import annotations
from typing import Any, Dict, Iterable, List, Optional, Callable

from .util import node_text, child_text_by_type, heading_text
from .xml import xml_start_tag, xml_tag_name, xml_attr_value, xml_fallback_tag_name

# ---- types ----
EmitFn = Callable[[str, Any], None]
DescendantLookup = Callable[[Any, Optional[str]], Optional[str]]
FieldNameText = Callable[[bytes, Any], Optional[str]]

def _as_name(x: Any) -> str:
    """Coerce a 'name' that might be str | list[str] | tuple[...] | None -> str."""
    if x is None:
        return ""
    if isinstance(x, (list, tuple, set)):
        for s in x:
            if s:  # first truthy
                return str(s)
        return ""
    return str(x)

def _field_name_text(utf8: bytes, node) -> str | None:
    # Prefer field-based names if present (TS, JS, Python, Java have them)
    for fname in ("name", "identifier"):
        ch = getattr(node, "child_by_field_name", lambda *_: None)(fname)
        if ch:
            return node_text(utf8, ch).strip()
    return None

def normalize_sym(sym: Any) -> str:
    """Force any symbol-like value into a stable string."""
    if sym is None:
        return ""
    if isinstance(sym, (list, tuple, set)):
        return ".".join(str(s) for s in sym if s is not None)
    return str(sym)

def _first_name_from_candidates(utf8, node, name_child, _descendant_text_by_type):
    """
    Return the first matching name string given a name_child which may be a str or
    a sequence of candidate node-type names. Always returns a str or None.
    """
    if not name_child:
        return None

    def try_one(type_name: str) -> Optional[str]:
        return (
            child_text_by_type(utf8, node, type_name)
            or _descendant_text_by_type(utf8, node, type_name)
        )

    if isinstance(name_child, (list, tuple, set)):
        for t in name_child:
            txt = try_one(t)
            if txt:
                return txt
        return None
    else:
        return try_one(name_child)

def _descendant_text_by_type(utf8, node, type_name: Optional[str]) -> Optional[str]:
    if not type_name:
        return None
    q = list(getattr(node, "children", []))
    while q:
        n = q.pop(0)
        if n.type == type_name:
            return node_text(utf8, n).strip()
        q.extend(getattr(n, "children", []))
    return None


def walk_code(
        node,
        *,
        utf8:bytes,
        spec: Any | dict[str, Any] | None,
        emit:EmitFn,
        container_sym: Optional[str] = None
        ):
    class_types: set[str] = spec["class"]
    method_types: set[str] = spec["method"]
    name_child: Any = spec["name_child"]

    def _walk(node, container_sym: Optional[str] = None):
        container_norm = normalize_sym(container_sym)
        next_container = container_norm or None


        if node.type in class_types:
            # cname_raw = _first_name_from_candidates(utf8, node, name_child, _descendant_text_by_type)
            cname = _field_name_text(utf8, node) or _as_name(
                _first_name_from_candidates(utf8, node, name_child, _descendant_text_by_type)
            )
            sym = cname or container_norm
            sym = normalize_sym(sym)
            emit(sym, node)
            next_container = sym
        elif node.type in method_types:
            mname = _field_name_text(utf8, node) or _as_name(
                _first_name_from_candidates(utf8, node, name_child, _descendant_text_by_type)
                )

            if container_norm and mname:
                sym = f"{container_norm}.{mname}"
            else:
                sym = mname or container_norm
            sym = normalize_sym(sym)
            emit(sym, node)

        for ch in getattr(node, "children", []):
            _walk(ch, next_container)

    _walk(node, container_sym)

    # # markdown
def walk_markdown(node, *, utf8, extra, emit):
    heads = extra.get("heading_nodes", set())
    def _walk(node):
        if node.type in heads:
            emit(heading_text(utf8, node), node)
        for ch in getattr(node, "children", []):
            _walk(ch)
    _walk(node)

def walk_xml(node, *, utf8, extra, emit, ns: Optional[str] = None):
    import re

    xml_cfg = extra.get("xml", {})
    def_elems: set[str] = xml_cfg.get("def_elements", set())
    name_attrs: List[str] = xml_cfg.get("name_attrs", [])
    ns_attr: Optional[str] = xml_cfg.get("ns_attr")

    def _walk(node, current_ns:Optional[str] = None):
        next_ns = current_ns
        if node.type == "element":
            tag = xml_tag_name(utf8, node) or xml_fallback_tag_name(utf8, node)
            st = xml_start_tag(node)

            # --- namespace detection (attribute first, then regex fallback) ---
            ns_val = None
            if st and ns_attr:
                ns_val = xml_attr_value(utf8, st, ns_attr)
            if ns_val is None:
                raw = node_text(utf8, st or node)
                m = re.search(r'namespace\s*=\s*"([^"]+)"', raw)
                if m:
                    ns_val = m.group(1)
            if ns_val:
                next_ns = ns_val

            # --- emit mapper only for the <mapper> tag ---
            if tag == "mapper":
                emit(f"{next_ns}.mapper" if next_ns else "mapper", node)

            # --- emit MyBatis defs like mapper.select#id, mapper.resultMap#id ---
            if tag and (tag in def_elems):
                ident = None
                # prefer attribute extractor
                if st:
                    for a in name_attrs:
                        ident = xml_attr_value(utf8, st, a)
                        if ident:
                            break
                # fallback: regex on raw start tag text
                if not ident:
                    raw = node_text(utf8, st or node)
                    for a in name_attrs:
                        m = re.search(rf'{re.escape(a)}\s*=\s*"([^"]+)"', raw)
                        if m:
                            ident = m.group(1)
                            break

                parts = []
                if tag != "mapper":
                    parts.append("mapper")
                parts.append(tag if not ident else f"{tag}#{ident}")
                sym = ".".join(parts)
                if next_ns:
                    sym = f"{next_ns}.{sym}"
                emit(sym, node)

        for ch in getattr(node, "children", []):
            _walk(ch, next_ns)
    _walk(node, ns)