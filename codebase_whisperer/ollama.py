# codebase_whisperer/ollama.py
from __future__ import annotations
import json
import time
from typing import Dict, List, Optional, Callable
import requests

class OllamaError(RuntimeError): ...

class OllamaClient:
    def __init__(self, host: str, timeout: float = 30.0, retries: int = 2, backoff: float = 0.5):
        self.host = host.rstrip("/")
        self.timeout = timeout
        self.retries = retries
        self.backoff = backoff

    def _post(self, path: str, payload: Dict, stream: bool = False) -> requests.Response:
        url = f"{self.host}{path}"
        for attempt in range(self.retries + 1):
            try:
                resp = requests.post(url, json=payload, timeout=self.timeout, stream=stream)
                resp.raise_for_status()
                return resp
            except requests.exceptions.RequestException as e:
                if attempt == self.retries:
                    raise OllamaError( f"Ollama POST {path} failed after {self.retries + 1} attempts: {e}") from e
                time.sleep(self.backoff * (2 ** attempt))
                continue
        return None


    def embed(self, model: str, texts: List[str]) -> List[List[float]]:
        vecs = []
        for t in texts:
            resp = self._post("/api/embeddings", {"model": model, "prompt": t})
            vecs.append(resp.json()["embedding"])
        return vecs

    def chat(
            self, model: str,
            messages: List[Dict[str, str]],
            stream: bool = True,
            on_chunk: Optional[Callable[[str], None]] = None,
            ) -> str:
        """Chat with the model. If stream=True, optionally emit partial chunks via on_chunk."""
        payload = {"model": model, "messages": messages, "stream": stream}

        if not stream:
            resp = self._post("/api/chat", payload, stream=False)
            return resp.json()["message"]["content"]

        #streaming
        resp = self._post("/api/chat", payload, stream=True)
        out: List[str] = []
        for line in resp.iter_lines(decode_unicode=True):
            if not line:
                continue
            obj = json.loads(line)
            piece = obj.get("message", {}).get("content")
            if piece:
                if on_chunk:
                    on_chunk(piece)
                out.append(piece)
        return "".join(out)
