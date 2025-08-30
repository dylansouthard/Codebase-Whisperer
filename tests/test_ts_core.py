# tests/test_ts_core.py
import pytest

from codebase_whisperer.chunking import get_ts_parser, extract_defs, chunk_defs_with_limits

def _need(lang: str):
    parser, _ = get_ts_parser(lang)
    if not parser:
        pytest.skip(f"tree-sitter parser not available for {lang}")
    return parser

# -----------------------
# Code-ish languages
# -----------------------

def test_java_class_and_method_symbols():
    parser = _need("java")
    src = """
    package com.example;
    public class Foo {
        private int x;
        public Foo() {}
        public String bar(int n) { return "" + n; }
    }
    """
    defs = extract_defs("java", parser, src)
    # collect symbols
    syms = [s for s,_ in defs]
    assert "Foo" in syms
    # container.method
    assert "Foo.bar" in syms
    # snippets should contain code-ish text
    assert any("class Foo" in code for _, code in defs)
    assert any("String bar" in code for _, code in defs)

def test_typescript_function_and_class():
    parser = _need("typescript")
    src = """
    export class Greeter {
      greet(name: string) { return `hi ${name}`; }
    }
    export function topLevel(a: number): number { return a + 1; }
    """
    defs = extract_defs("typescript", parser, src)
    syms = [s for s,_ in defs]
    assert "Greeter" in syms
    assert "Greeter.greet" in syms
    assert "topLevel" in syms

def test_python_defs():
    parser = _need("python")
    src = """
    class A:
        def m(self): return 1

    def top(): pass
    """
    defs = extract_defs("python", parser, src)
    syms = [s for s,_ in defs]
    assert "A" in syms
    assert "A.m" in syms
    assert "top" in syms

# -----------------------
# Markdown
# -----------------------

def test_markdown_headings():
    parser = _need("markdown")
    src = """
# Title

Some text

## Subhead

More text
"""
    defs = extract_defs("markdown", parser, src)
    # Symbols are the normalized heading text
    syms = [s for s,_ in defs]
    assert "Title" in syms
    assert "Subhead" in syms

# -----------------------
# XML / MyBatis
# -----------------------

def test_mybatis_namespace_and_select():
    parser = _need("xml")
    src = """
<mapper namespace="com.foo.UserMapper">
  <resultMap id="UserMap" type="User"/>
  <select id="findById" resultMap="UserMap">
    SELECT * FROM users WHERE id = #{id}
  </select>
</mapper>
"""
    defs = extract_defs("xml", parser, src)
    syms = [s for s,_ in defs]
    # mapper emits with namespace
    assert "com.foo.UserMapper.mapper" in syms
    # select emits mapper.select#id with namespace prefix
    assert "com.foo.UserMapper.mapper.select#findById" in syms
    # resultMap is included too
    assert "com.foo.UserMapper.mapper.resultMap#UserMap" in syms

# -----------------------
# Fallback / Unknown
# -----------------------

def test_unknown_language_graceful_fallback():
    # Use a real parser but a lang key that doesn't exist in LANG_NODE_MAP
    parser = _need("python")
    src = "print('hello')"
    defs = extract_defs("weirdlang", parser, src)
    # We should still get at least one snippet (empty symbol allowed)
    assert defs
    assert isinstance(defs[0][0], str)  # symbol
    assert isinstance(defs[0][1], str)  # snippet

# -----------------------
# Descendant identifier lookup
# -----------------------

def test_descendant_identifier_lookup_not_direct_child():
    # Craft TS where function name may be nested (e.g. export default function name() {})
    parser = _need("typescript")
    src = """
    export default function deeplyNestedName() { return 1; }
    """
    defs = extract_defs("typescript", parser, src)
    syms = [s for s,_ in defs]
    # Depending on grammar, either the function symbol is captured as name or empty.
    # We assert that *some* function-like def is emitted.
    assert any("deeplyNestedName" in s for s in syms) or any(code.startswith("export default function") for _,code in defs)

# -----------------------
# De-duplication
# -----------------------

def test_deduplicates_same_symbol_and_code_chunk():
    parser = _need("python")
    src = """
def f(): pass
def f(): pass
"""
    defs = extract_defs("python", parser, src)
    # Even if two identical defs appear (synthetically), we should not emit duplicates
    seen = set()
    for sym, code in defs:
        key = (sym, code[:120])
        assert key not in seen
        seen.add(key)

# -----------------------
# Chunking behavior
# -----------------------

def test_chunk_defs_with_limits_splits_large_defs():
    # fake defs: one large, one small
    big = "X" * 2500
    defs = [("Foo.bar", big), ("Foo", "class Foo {}")]
    chunks = chunk_defs_with_limits(defs, max_chars=1000)
    # big splits into 3 chunks (1000,1000,500), small stays 1
    sizes = [len(c[0]) for c in chunks]
    assert sizes.count(1000) == 2
    assert any(s <= 1000 for s in sizes)
    # symbols preserved
    syms = [s for _, s in chunks]
    assert "Foo.bar" in syms and "Foo" in syms