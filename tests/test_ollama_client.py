# tests/test_ollama_client.py
import json
import types
import requests
import pytest

from codebase_whisperer.llm.ollama import OllamaClient, OllamaError

class FakeResponse:
    """Minimal stand-in for requests.Response."""
    def __init__(self, *, status=200, json_data=None, lines=None):
        self.status_code = status
        self._json_data = json_data or {}
        # lines: iterable of str (already unicode) for streaming mode
        self._lines = list(lines or [])

    def raise_for_status(self):
        if self.status_code >= 400:
            # create an HTTPError like requests would
            raise requests.exceptions.HTTPError(f"{self.status_code} error")

    def json(self):
        return self._json_data

    def iter_lines(self, decode_unicode=False):
        # requests yields lines split by \n; our test lines are already per-line
        for line in self._lines:
            if line is None:
                yield b"" if not decode_unicode else ""
            else:
                yield line if decode_unicode else line.encode("utf-8")

@pytest.fixture(autouse=True)
def no_sleep(monkeypatch):
    """Avoid real sleeping during backoff."""
    monkeypatch.setattr("time.sleep", lambda *_args, **_kwargs: None)

def test__post_success(monkeypatch):
    calls = {}
    def fake_post(url, json=None, timeout=None, stream=None, **kwargs):
        calls["last"] = (url, json, timeout, stream)
        return FakeResponse(status=200, json_data={"ok": True})
    monkeypatch.setattr(requests, "post", fake_post)

    c = OllamaClient("http://localhost:11434", timeout=1, retries=0)
    resp = c._post("/api/ping", {"x": 1})
    assert resp.json()["ok"] is True
    assert calls["last"][0].endswith("/api/ping")
    assert calls["last"][2] == 1

def test__post_retries_then_success(monkeypatch):
    attempts = {"n": 0}
    def flaky_post(url, json=None, timeout=None, stream=None, **kwargs):
        attempts["n"] += 1
        if attempts["n"] == 1:
            raise requests.exceptions.ConnectionError("boom")
        return FakeResponse(status=200, json_data={"ok": True})
    monkeypatch.setattr(requests, "post", flaky_post)

    c = OllamaClient("http://localhost:11434", retries=1, backoff=0)
    resp = c._post("/api/ping", {"x": 1})
    assert resp.json()["ok"] is True
    assert attempts["n"] == 2  # retried once

def test__post_exhausts_retries_raises(monkeypatch):
    def always_fail(url, json=None, timeout=None, stream=None, **kwargs):
        raise requests.exceptions.Timeout("slow")
    monkeypatch.setattr(requests, "post", always_fail)

    c = OllamaClient("http://localhost:11434", retries=2, backoff=0)
    with pytest.raises(OllamaError) as ei:
        c._post("/api/ping", {"x": 1})
    assert "failed after 3 attempts" in str(ei.value)

def test_embed_multiple_texts(monkeypatch):
    calls = {"count": 0, "payloads": []}

    def post_embeddings(url, json=None, timeout=None, stream=None, **kwargs):
        calls["count"] += 1
        calls["payloads"].append(json)
        # Return a 2-dim vector to make assertions simple
        return FakeResponse(status=200, json_data={"embedding": [0.1, 0.2]})

    monkeypatch.setattr(requests, "post", post_embeddings)

    c = OllamaClient("http://localhost:11434")
    vecs = c.embed("nomic-embed-text", ["a", "b", "c"])
    assert len(vecs) == 3
    assert vecs[0] == [0.1, 0.2]
    assert calls["count"] == 3
    assert calls["payloads"][0]["model"] == "nomic-embed-text"
    assert calls["payloads"][0]["prompt"] == "a"

def test_chat_non_stream(monkeypatch):
    def post_chat(url, json=None, timeout=None, stream=None, **kwargs):
        return FakeResponse(status=200, json_data={"message": {"content": "hello"}})
    monkeypatch.setattr(requests, "post", post_chat)

    c = OllamaClient("http://localhost:11434")
    out = c.chat("llama3:8b", [{"role": "user", "content": "hi"}], stream=False)
    assert out == "hello"

def test_chat_stream_emits_and_collects(monkeypatch):
    # Simulate NDJSON streaming lines from Ollama
    lines = [
        json.dumps({"message": {"content": "Hello "}}),
        json.dumps({"message": {"content": "world"}}),
        json.dumps({"message": {"content": "!"}}),
        json.dumps({"done": True}),
    ]
    def post_chat(url, json=None, timeout=None, stream=None, **kwargs):
        assert stream is True
        return FakeResponse(status=200, lines=lines)
    monkeypatch.setattr(requests, "post", post_chat)

    chunks = []
    def on_chunk(s: str):
        chunks.append(s)

    c = OllamaClient("http://localhost:11434")
    full = c.chat(
        "llama3:8b",
        [{"role": "user", "content": "greet me"}],
        stream=True,
        on_chunk=on_chunk,
    )
    assert full == "Hello world!"
    assert chunks == ["Hello ", "world", "!"]

def test_chat_stream_handles_non_message_lines(monkeypatch):
    lines = [
        json.dumps({"message": {"content": "A"}}),
        json.dumps({"progress": 0.5}),  # no message key
        json.dumps({"message": {"content": "B"}}),
        json.dumps({"done": True}),
    ]
    def post_chat(url, json=None, timeout=None, stream=None, **kwargs):
        return FakeResponse(status=200, lines=lines)
    monkeypatch.setattr(requests, "post", post_chat)

    c = OllamaClient("http://localhost:11434")
    full = c.chat("llama3:8b", [{"role": "user", "content": "x"}], stream=True)
    assert full == "AB"
