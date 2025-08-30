# codebase_whisperer/chunking/lang_nodes.py
from __future__ import annotations

"""
Language node/spec config for Tree-sitter extraction.
- 'class': node types that define a container / type
- 'method': node types that define callable units (functions/methods/ctors)
- 'name_child': the child node type that holds the identifier (varies by grammar)
- Optional 'extra':
    - 'container_walk': True -> recursively scan inside class nodes for methods
    - 'heading_nodes': for markdown-like headings
    - 'xml': { 'def_elements': {...}, 'name_attrs': ['id','name'], 'ns_attr': 'namespace' }
"""

LANG_NODE_MAP = {
    # -------- JVM family --------
    "java": {
        "class":  {"class_declaration", "interface_declaration", "enum_declaration"},
        "method": {"method_declaration", "constructor_declaration"},
        "name_child": "identifier",
        "extra": {"container_walk": True},
    },
    "kotlin": {
        "class":  {"class_declaration", "object_declaration", "interface_declaration"},
        "method": {"function_declaration", "secondary_constructor"},
        "name_child": "simple_identifier",
        "extra": {"container_walk": True},
    },
    "groovy": {
        "class":  {"class_declaration"},
        "method": {"method_declaration"},
        "name_child": "identifier",
        "extra": {"container_walk": True},
    },

    # -------- Web --------
    "typescript": {
        "class":  {"class_declaration"},
        "method": {"function_declaration", "method_definition"},
        "name_child": "identifier",
        "extra": {"container_walk": True},
    },
    "tsx": {  # treat same as ts for symbols
        "class":  {"class_declaration"},
        "method": {"function_declaration", "method_definition"},
        "name_child": "identifier",
        "extra": {"container_walk": True},
    },
    "javascript": {
        "class":  {"class_declaration"},
        "method": {"function_declaration", "method_definition"},
        "name_child": "identifier",
        "extra": {"container_walk": True},
    },

    # -------- Python --------
    "python": {
        "class":  {"class_definition"},
        "method": {"function_definition"},
        "name_child": "identifier",
        "extra": {"container_walk": True},
    },

    # -------- XML (MyBatis-aware) --------
    "xml": {
        # treat elements as defs; we’ll pick the interesting MyBatis ones out
        "class":  {"element"},   # container-ish
        "method": set(),         # we’ll synthesize symbols from element names
        "name_child": None,
        "extra": {
            "xml": {
                # MyBatis statements:
                "def_elements": {
                    "mapper", "select", "insert", "update", "delete", "resultMap",
                    "sql", "include", "where", "set", "trim", "foreach", "choose", "when", "otherwise",
                },
                # attributes whose value acts like an identifier:
                "name_attrs": ["id", "resultMap", "parameterMap"],
                # namespace carried at <mapper namespace="...">
                "ns_attr": "namespace",
            }
        },
    },

    # -------- YAML --------
    "yaml": {
        "class":  {"block_mapping", "block_node"},
        "method": set(),
        "name_child": None,
        "extra": {},
    },

    # -------- Markdown --------
    "markdown": {
        "class": set(),
        "method": set(),
        "name_child": None,
        "extra": {
            "heading_nodes": {"atx_heading", "setext_heading"},
        },
    },
}