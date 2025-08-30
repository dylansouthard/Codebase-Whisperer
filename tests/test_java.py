# tests/test_java_chunker.py
from codebase_whisperer.chunking.java import chunk_java, _symbol_from_signature

# --- helpers ---------------------------------------------------------------

def _symbols(chunks):
    """Return the list of symbols (method names) present in chunk tuples."""
    return [sym for _, sym in chunks if sym]

def _method_chunks(chunks, name):
    """All chunks that belong to a given method symbol."""
    return [c for (c, s) in chunks if s == name]

def _no_chunk_exceeds(chunks, max_chars):
    return all(len(c) <= max_chars for c, _ in chunks)

# --- unit tests ------------------------------------------------------------

def test_symbol_extraction_basic():
    sigs = [
        "public static void main(String[] args) {",
        "private List<String> getNames() throws IOException {",
        "protected int add(int a, int b) {"
    ]
    names = [_symbol_from_signature(s) for s in sigs]
    assert names == ["main", "getNames", "add"]

def test_simple_class_two_methods_and_limits():
    java = """
        package demo;

        public class Foo {
            // preamble stuff
            private int field = 42;

            public void alpha() {
                System.out.println("alpha");
            }

            private String beta(int x) throws IllegalStateException {
                if (x > 0) { return "pos"; } else { return "neg"; }
            }
        }
    """
    max_chars = 120
    chunks = chunk_java(java, max_chars=max_chars)

    # We should see method symbols
    syms = _symbols(chunks)
    assert "alpha" in syms
    assert "beta" in syms

    # All chunks obey size cap (regex chunker uses split_by_size)
    assert _no_chunk_exceeds(chunks, max_chars)

    # Content sanity: method-specific chunks contain their signature/body
    alpha_chunks = _method_chunks(chunks, "alpha")
    assert any("public void alpha()" in c for c in alpha_chunks)
    assert any("alpha" in c for c in alpha_chunks)

    beta_chunks = _method_chunks(chunks, "beta")
    assert any("private String beta" in c for c in beta_chunks)
    assert any("throws IllegalStateException" in c for c in beta_chunks)

def test_generics_and_throws_signature_is_captured():
    java = """
        public class G {
            public <T extends Number> T compute(T a, T b) throws java.io.IOException {
                return a;
            }
        }
    """
    chunks = chunk_java(java, max_chars=200)
    syms = _symbols(chunks)
    assert "compute" in syms

def test_nested_braces_do_not_truncate_method():
    java = """
        public class Nest {
            public int calc(int n) {
                int s = 0;
                for (int i = 0; i < n; i++) {
                    if (i % 2 == 0) { s += i; } else { s -= i; }
                }
                return s;
            }
        }
    """
    chunks = chunk_java(java, max_chars=160)
    calc = "".join(_method_chunks(chunks, "calc"))
    # Should include both the for-loop and the final return
    assert "for (int i = 0; i < n; i++)" in calc
    assert "return s;" in calc

def test_long_method_is_split_into_multiple_chunks_with_same_symbol():
    body_line = "0123456789" * 30  # 300 chars
    java = f"""
        public class Longy {{
            public void huge() {{
                // start
                {body_line}
                {body_line}
                {body_line}
                // end
            }}
        }}
    """
    max_chars = 180
    chunks = chunk_java(java, max_chars=max_chars)
    huge_chunks = _method_chunks(chunks, "huge")
    # Should be split across multiple pieces
    assert len(huge_chunks) >= 2
    assert all(len(c) <= max_chars for c in huge_chunks)

def test_multiple_classes_in_one_file_are_both_seen():
    java = """
        public class A {
            public void a1() {}
        }

        final class B {
            private static int b2() { return 2; }
        }
    """
    chunks = chunk_java(java, max_chars=160)
    syms = set(_symbols(chunks))
    assert {"a1", "b2"}.issubset(syms)

def test_interface_without_bodies_falls_back_to_plain_chunks():
    # Interface method declarations lack bodies ("{") unless default,
    # so regex method matcher won't capture them as methods.
    java = """
        public interface C {
            void f();            // declaration only
            default void g() { } // has a body
        }
    """
    chunks = chunk_java(java, max_chars=120)
    syms = _symbols(chunks)
    # default method is captured, declaration-only isn't
    assert "g" in syms
    assert "f" not in syms
    # There should still be some non-symbol chunks (class/interface preamble)
    assert any(sym is None for _, sym in chunks)

def test_no_methods_entire_file_becomes_plain_chunks():
    java = """
        package p;
        // no classes or methods here, just commentary
        /* and a block comment */
    """
    chunks = chunk_java(java, max_chars=80)
    # No symbols and at least one chunk of text
    assert all(sym is None for _, sym in chunks)
    assert len(chunks) >= 1
