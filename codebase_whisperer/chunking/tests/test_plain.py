# tests/test_chunk_plain.py

from ..plain import chunk_plain

def test_single_short_paragraph_no_split():
    text = "hello world"
    chunks = chunk_plain(text, max_chars=100, min_chars=1)
    assert chunks == ["hello world"]

def test_two_paragraphs_merge_when_under_max():
    p1 = "A" * 20
    p2 = "B" * 20
    # p1 + "\n\n" + p2 = 42 chars <= max_chars → single chunk
    text = f"{p1}\n\n{p2}"
    chunks = chunk_plain(text, max_chars=60, min_chars=1)
    assert len(chunks) == 1
    assert chunks[0] == f"{p1}\n\n{p2}"

def test_split_when_next_paragraph_would_exceed_max():
    p1 = "A" * 50
    p2 = "B" * 50
    # 50 + 2 + 50 = 102 > 80 → must split into two chunks
    text = f"{p1}\n\n{p2}"
    chunks = chunk_plain(text, max_chars=80, min_chars=1)
    assert chunks == [p1, p2]

def test_hard_split_long_token_no_whitespace():
    # Simulate minified junk: a single huge "paragraph" with no spaces/newlines
    long = "x" * 2300
    chunks = chunk_plain(long, max_chars=1000, min_chars=1)
    # Should be split into 1000/1000/300 by split_by_size
    assert [len(c) for c in chunks] == [1000, 1000, 300]
    assert "".join(chunks) == long  # no data loss

def test_min_merge_can_exceed_max():
    # Three small paragraphs; initial pass yields 3 chunks (each 100)
    # min_chars=250 should merge them into a single ~304-char chunk
    p1 = "a" * 100
    p2 = "b" * 100
    p3 = "c" * 100
    text = f"{p1}\n\n{p2}\n\n{p3}"
    chunks = chunk_plain(text, max_chars=150, min_chars=250)
    assert len(chunks) == 1
    merged = chunks[0]
    # It’s OK if the merged chunk exceeds max_chars — by design.
    assert len(merged) >= 250
    # And it should contain all three paras with double-newline separators
    assert merged == f"{p1}\n\n{p2}\n\n{p3}"

def test_multiple_blank_lines_collapsed_by_split():
    text = "Top\n\n\n\nMiddle\n\nBottom"
    chunks = chunk_plain(text, max_chars=1000, min_chars=1)
    # chunk_plain splits by blank-line groups; content is preserved per paragraph.
    assert chunks == ["Top\n\nMiddle\n\nBottom"]

def test_empty_input_returns_empty_list():
    text = "   \n   \n"
    chunks = chunk_plain(text, max_chars=100, min_chars=1)
    assert chunks == []

def test_preserves_order_and_content_after_splitting_and_merging():
    parts = ["alpha", "beta", "gamma", "delta"]
    text = "\n\n".join(parts)
    # Force initial split into separate chunks (low max), then merge to min
    chunks = chunk_plain(text, max_chars=10, min_chars=15)
    # Should end up with two merged chunks: "alpha\n\nbeta" and "gamma\n\ndelta"
    assert chunks == ["alpha\n\nbeta\n\ngamma", "delta"]
