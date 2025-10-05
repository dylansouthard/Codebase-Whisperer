# codebase_whisperer/ollama.py
from __future__ import annotations

import json
import time
import os
import sys
from typing import Any, Callable, Dict, Iterable, List, Optional

import requests

def _olog(*a, **kw):
    if os.environ.get("OLLAMA_DEBUG") == "1":
        print(*a, file=sys.stderr, **kw)

class OllamaError(Exception):
    def __init__(self, msg: str, *, status: Optional[int] = None, body: Optional[str] = None):
        super().__init__(msg)
        self.status = status
        self.body = body


def _is_transient(status: Optional[int], err: Optional[BaseException]) -> bool:
    if err is not None:
        # connection / timeouts considered transient
        return isinstance(err, (requests.exceptions.ConnectionError, requests.exceptions.Timeout))
    if status is None:
        return False
    # HTTP 429 / 5xx are transient
    return status == 429 or 500 <= status < 600


class OllamaClient:
    """
    Minimal, black-box client:
      - _post(): retrying POST wrapper
      - embed(): returns list[list[float]] (one call per input, stable behavior)
      - chat(): returns string; supports streaming with on_chunk callback
    """

    def __init__(
        self,
        host: str,
        *,
        timeout: float = 30.0,
        retries: int = 2,
        backoff: float = 0.25,
        headers: Optional[Dict[str, str]] = None,
    ) -> None:
        self.host = host.rstrip("/")
        self.timeout = timeout
        self.retries = retries
        self.backoff = backoff
        self._headers = {"Content-Type": "application/json", **(headers or {})}

    # ---- low-level ---------------------------------------------------------
    def _post(self, path: str, payload: Dict[str, Any], *, stream: bool = False) -> requests.Response:
        url = f"{self.host}{path}"
        last_err: Optional[BaseException] = None

        for attempt in range(self.retries + 1):
            try:
                _olog(f"[OLLAMA] POST {url} attempt={attempt} stream={stream}")
                _olog(f"[OLLAMA] payload={json.dumps(payload)[:500]}...")
                # NOTE: tests monkeypatch requests.post; call it directly.
                resp = requests.post(url, json=payload, timeout=self.timeout, headers=self._headers, stream=stream)
                _olog(f"[OLLAMA] status={resp.status_code}")

                if 200 <= resp.status_code < 300:
                    if stream:
                        _olog("[OLLAMA] success (stream=True); not peeking body")
                        return resp
                    # Peek at body on success (especially useful for /api/chat non-stream)
                    try:
                        body_sample = resp.text[:800]
                    except Exception:
                        body_sample = "<unreadable>"
                    _olog(f"[OLLAMA] success body-sample={body_sample!r}")
                    return resp

                body_text = None
                try:
                    body_text = resp.text
                except Exception:
                    pass
                _olog(f"[OLLAMA] non-2xx body={str(body_text)[:500]}...")

                if attempt == self.retries or not _is_transient(resp.status_code, None):
                    raise OllamaError(
                        f"Ollama POST {path} failed: {resp.status_code}",
                        status=resp.status_code,
                        body=body_text,
                    )
            except (requests.exceptions.ConnectionError, requests.exceptions.Timeout) as e:
                last_err = e
                if attempt == self.retries:
                    raise OllamaError(f"Ollama POST {path} failed after {self.retries + 1} attempts") from e
            # backoff for next attempt
            time.sleep(self.backoff * (2 ** attempt))

        # Should not reach here
        raise OllamaError(
            f"Ollama POST {path} failed after {self.retries + 1} attempts",
            body=str(last_err) if last_err else None,
        )

    # ---- high-level --------------------------------------------------------
    def embed(self, model: str, texts: Iterable[str]) -> List[List[float]]:
        """
        Deterministic behavior for tests: one POST per input text.
        Returns a list of vectors (same order as inputs).
        """
        out: List[List[float]] = []
        for t in texts:
            resp = self._post("/api/embeddings", {"model": model, "prompt": t}, stream=False)
            data = _safe_json(resp)
            _olog(f"[OLLAMA] chat non-stream raw={json.dumps(data)[:800]}...")
            vec = data.get("embedding") or []
            _olog(f"[OLLAMA] embed model={model} len={len(vec)} first5={vec[:5] if isinstance(vec, list) else 'N/A'}")
            out.append(vec)
        return out

    def chat(
        self,
        model: str,
        messages: List[Dict[str, Any]],
        *,
        stream: bool = False,
        on_chunk: Optional[Callable[[str], None]] = None,
        options: Optional[Dict[str, Any]] = None,
    ) -> str:
        payload = {"model": model, "messages": messages, "stream": bool(stream)}
        if options:
            payload["options"] = options

        # -------- non-streaming (robust to NDJSON responses) --------
        if not stream:
            payload["stream"] = False
            resp = self._post("/api/chat", payload, stream=False)

            # Some Ollama builds return NDJSON even when stream=false.
            # Try JSON first; if that fails or looks empty, fall back to NDJSON parse.
            txt = resp.text or ""
            data = _safe_json(resp)
            msg = (data.get("message") or {}).get("content") if isinstance(data, dict) else None
            fallback = data.get("response") if isinstance(data, dict) else None

            if isinstance(msg, str) and msg.strip():
                return msg
            if isinstance(fallback, str) and fallback.strip():
                return fallback

            # NDJSON fallback: collect all lines and join
            out_parts: List[str] = []
            for line in (l for l in txt.splitlines() if l.strip()):
                try:
                    obj = json.loads(line)
                except Exception:
                    continue
                if obj.get("done"):
                    break
                # Prefer message.content; fall back to response
                part = ((obj.get("message") or {}).get("content")) or obj.get("response")
                if isinstance(part, str) and part:
                    out_parts.append(part)
            return "".join(out_parts)

        # -------- streaming (handle both message.content and response) --------
        resp = self._post("/api/chat", payload, stream=True)
        full: List[str] = []
        for raw in resp.iter_lines(decode_unicode=True):
            if not raw:
                continue
            _olog(f"[OLLAMA] chat stream line={raw[:200]}...")
            try:
                obj = json.loads(raw)
            except Exception as e:
                _olog(f"[OLLAMA] chat stream JSON error: {e}")
                continue

            if obj.get("done"):
                _olog("[OLLAMA] chat stream done")
                break

            # Prefer OpenAI-like message.content, but accept top-level response too
            part = ((obj.get("message") or {}).get("content")) or obj.get("response")
            if not part:
                continue

            s = str(part)
            full.append(s)
            _olog(f"[OLLAMA] chat stream chunk={(s[:200] + '...') if len(s) > 200 else s}")
            if on_chunk:
                try:
                    on_chunk(s)
                except Exception as e:
                    _olog(f"[OLLAMA] on_chunk error: {e}")

        final = "".join(full)
        _olog(f"[OLLAMA] chat stream final={(final[:400] + '...') if len(final) > 400 else final}")
        return final


def _safe_json(resp: requests.Response) -> Dict[str, Any]:
    try:
        return resp.json() or {}
    except Exception:
        try:
            return json.loads(resp.text or "{}")
        except Exception:
            return {}