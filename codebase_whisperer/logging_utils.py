from __future__ import annotations
import sys, time
from dataclasses import dataclass

@dataclass
class StageTimer:
    name: str
    start: float = None
    extra: dict | None = None

    def __enter__(self):
        self.start = time.perf_counter()
        _log(f"[start] {self.name}", self.extra)
        return self

    def __exit__(self, exc_type, exc, tb):
        dur = time.perf_counter() - self.start
        status = "ok" if exc is None else f"err={exc_type.__name__}"
        _log(f"[done]  {self.name}", {"duration_s": round(dur, 3), "status": status, **(self.extra or {})})

def _log(msg:str, kv:dict | None = None):
    if kv:
        fields = " ".join(f"{k}={v}" for k, v in kv.items())
        print(f"{msg} {fields}", file=sys.stderr)

class CounterBar:
    def __init__(self, label: str, total: int | None = None, every: int = 50):
        self.label, self.total, self.every = label, total, every
        self.n = 0
        self._t0 = time.perf_counter()
        _log(f"{label}: 0/{total if total is not None else '?'}")

    def update(self, k: int = 1):
        self.n += k
        if self.n % self.every == 0:
            self._emit()

    def close(self):
        self._emit(final=True)

    def _emit(self, final:bool = False):
        dt = time.perf_counter() - self._t0
        rate = self.n / dt if dt > 0 else 0
        info = {"count": self.n, "rate_per_s": round(rate, 2)}
        if self.total is not None:
            info["total"] = self.total
        _log(self.label + (" [final]" if final else ""), info)

