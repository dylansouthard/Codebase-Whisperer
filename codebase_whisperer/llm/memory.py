from __future__ import annotations
from dataclasses import dataclass
from typing import List, Dict, Optional, Tuple, Set
import time, os, threading

RAG_DEBUG = os.getenv("RAG_DEBUG") == "1"

@dataclass
class MemoryConfig:
    # how many full (user,assistant) pairs to keep verbatim
    max_full_pairs: int = 2
    # how many most-recent single-turn summaries to include
    max_recent_summaries: int = 2
    # after how many accumulated unsummarized turns to absorb into digest
    summarize_every: int = 6
    # hard ceiling to trigger absorb as a safety
    max_turns: int = 200
    # cap for digest size (chars); trim oldest when exceeding
    digest_max_chars: int = 12_000
    # models
    summarizer_model: Optional[str] = None  # default: fall back to chat model
    # embeddings
    embed_turns: bool = False  # set True if you’ll use vector-based recall later
    #

class Memory:
    """
    Rolling conversation memory with incremental 'absorb & prune':
      - keep a few most-recent full pairs
      - keep a few recent summaries
      - absorb older content into a compact digest and prune the raw turns
    """

    def __init__(
        self,
        client,                 # OllamaClient
        chat_model: str,        # used if summarizer_model is None
        embed_model: str,
        pinned: Optional[List[str]] = None,
        cfg: Optional[MemoryConfig] = None,
    ):
        self.client = client
        self.chat_model = chat_model
        self.embed_model = embed_model
        self.pinned = pinned or []
        self.cfg = cfg or MemoryConfig()
        self.summarizer_model = self.cfg.summarizer_model or self.chat_model

        # concurrency controls for digest/summarization
        self._state_lock = threading.RLock()          # guards self.turns and self.digest_text
        self._absorb_flag_lock = threading.Lock()     # guards the flags below
        self._absorb_in_progress: bool = False
        self._absorb_pending: bool = False
        self._id_seq: int = 0                         # monotonically increasing turn id

        # live turns buffer (not yet absorbed into digest)
        # we store single turns, but treat the tail in pairs when building context
        self.turns: List[Dict] = []  # {role, text, ts, embedding?, summary?}
        # rolling digest of older content
        self.digest_text: str = ""

    # ---- public API ---------------------------------------------------------

    def append_pair(self, user_text: str, assistant_text: str) -> None:
        """
        Atomically append a complete (user, assistant) turn and trigger a single absorb.
        Use this to avoid double-triggering absorb per half-turn.
        """
        with self._state_lock:
            # user record
            urec = {
                "id": self._id_seq,
                "role": "user",
                "text": user_text,
                "ts": time.time(),
                "summary": None
            }
            self._id_seq += 1
            if self.cfg.embed_turns:
                urec["embedding"] = self.client.embed(self.embed_model, [user_text])[0]
            self.turns.append(urec)

            # assistant record
            arec = {
                "id": self._id_seq,
                "role": "assistant",
                "text": assistant_text,
                "ts": time.time(),
                "summary": None
            }
            self._id_seq += 1
            if self.cfg.embed_turns:
                arec["embedding"] = self.client.embed(self.embed_model, [assistant_text])[0]
            self.turns.append(arec)

        # Single non-blocking trigger for the whole pair
        self._maybe_absorb()

    def append(self, role: str, text: str) -> None:
        # Note: prefer append_pair() for normal chat cycles to avoid double-triggering absorb.
        rec = {"id": self._id_seq, "role": role, "text": text, "ts": time.time(), "summary": None}
        with self._state_lock:
            self._id_seq += 1
            if self.cfg.embed_turns:
                # embed outside the lock would be nicer, but we want the embedding tied to this exact text atomically
                rec["embedding"] = self.client.embed(self.embed_model, [text])[0]
            self.turns.append(rec)
        # decide if it's time to absorb older turns into the digest (non-blocking scheduler)
        self._maybe_absorb()

    def build_context(self, question: str) -> Dict[str, Optional[str]]:
        # ensure we don't carry giant raw buffers between calls and take a consistent snapshot
        # do not block on active summarization; we only read current state
        with self._state_lock:
            self._maybe_absorb(force_if_max=True)

            # 1) last N full pairs (verbatim)
            full_pairs = self._last_full_pairs(self.cfg.max_full_pairs)

            # 2) right before those, take up to K recent single-turn summaries
            recent_summaries = self._recent_summaries(before_pairs=len(full_pairs),
                                                      limit=self.cfg.max_recent_summaries)

            # 3) digest tail (already compact)
            digest = self._digest_tail()

            if RAG_DEBUG:
                from rich import print as rprint
                rprint("[dim]MEMORY pinned:[/dim]", self.pinned)
                rprint("[dim]MEMORY full_pairs_count:[/dim]", len(full_pairs))
                rprint("[dim]MEMORY summaries_count:[/dim]", len(recent_summaries))
                rprint("[dim]MEMORY digest_chars:[/dim]", len(digest or ""))

            return {
                "pinned": self.pinned,
                "full_pairs": full_pairs,
                "summaries": recent_summaries,
                "digest": digest,
            }

    def as_text_block(self, question: str) -> str:
        ctx = self.build_context(question)
        parts: List[str] = []
        if ctx["pinned"]:
            parts.append("Pinned:\n" + "\n".join(ctx["pinned"]))
        for u, a in ctx["full_pairs"]:
            parts.append(f"User: {u}\nAssistant: {a}")
        if ctx["summaries"]:
            parts.append("Recent summaries:\n" + "\n".join(ctx["summaries"]))
        if ctx["digest"]:
            parts.append("Digest:\n" + ctx["digest"])
        return "\n\n".join(parts)

    # ---- internals ----------------------------------------------------------

    def _maybe_absorb(self, *, force_if_max: bool = False) -> None:
        """
        Decide whether to run an absorb (summarize/merge) cycle.
        Never runs concurrently. If one is in-flight, we mark a single pending run.
        """
        # Take a quick snapshot length under lock
        with self._state_lock:
            n = len(self.turns)

        if n == 0:
            return

        trigger = (n >= self.cfg.summarize_every) or (force_if_max and n >= self.cfg.max_turns)
        if not trigger:
            return

        with self._absorb_flag_lock:
            if self._absorb_in_progress:
                # one in-flight; ensure exactly one queued follow-up
                self._absorb_pending = True
                return
            # no absorb active; start one
            self._absorb_in_progress = True
            threading.Thread(target=self._absorb_worker_loop, daemon=True).start()

    def _absorb_worker_loop(self) -> None:
        """
        Runs one or more absorb cycles back-to-back.
        If new work was requested while running, we perform exactly one additional cycle.
        """
        try:
            while True:
                self._absorb_one_cycle()
                # check if another run was requested during processing
                with self._absorb_flag_lock:
                    if self._absorb_pending:
                        self._absorb_pending = False
                        # loop again to absorb any newly eligible turns
                        continue
                    else:
                        # clear in-progress and exit
                        self._absorb_in_progress = False
                        break
        except Exception:
            # on any failure, make sure we clear the in-progress flag so future runs aren't blocked
            with self._absorb_flag_lock:
                self._absorb_in_progress = False
                self._absorb_pending = False

    def _absorb_one_cycle(self) -> None:
        """
        Absorb/prune based on a snapshot. We do LLM work outside the state lock.
        We do not rely on slice indexes; we remove absorbed turns by their IDs.
        """
        # --- snapshot which turns to absorb ---
        with self._state_lock:
            n = len(self.turns)
            if n == 0:
                return

            keep_tail = 2 * self.cfg.max_full_pairs + self.cfg.max_recent_summaries
            absorb_upto_count = max(0, n - keep_tail)

            # Need at least two turns to make absorption meaningful
            if absorb_upto_count < 2:
                return

            # collect the exact turns (by id) to absorb
            to_absorb = self.turns[:absorb_upto_count]
            absorb_ids: Set[int] = {t["id"] for t in to_absorb}
            # role-tagged text for the LLM
            chunk_lines = [f"{t['role'].capitalize()}: {t['text']}" for t in to_absorb]
            chunk_text = "\n".join(chunk_lines)
            existing_digest = self.digest_text

        # --- LLM work outside lock ---
        new_digest = self._merge_into_digest(existing_digest, chunk_text)
        if not new_digest:
            return

        # --- commit results and prune by id ---
        with self._state_lock:
            self.digest_text = new_digest
            # enforce digest cap
            if len(self.digest_text) > self.cfg.digest_max_chars:
                overflow = len(self.digest_text) - self.cfg.digest_max_chars
                self.digest_text = self.digest_text[overflow:]

            # prune absorbed turns by ID (turns may have grown since snapshot; IDs are stable)
            self.turns = [t for t in self.turns if t["id"] not in absorb_ids]

    def _summarize_turn(self, role: str, text: str) -> str:
        """
        Create a brief, role-aware single-turn summary.
        Output: 1–3 sentences capturing the intent/info of that turn.
        """
        msgs = [
            {"role": "system", "content": (
                "You write concise, role-aware summaries of single turns in a conversation. "
                "Start with who spoke (User or Assistant) and summarize their intent or key info. "
                "Keep it to 1–3 sentences. Avoid speculation."
            )},
            {"role": "user", "content": f"Role: {role.capitalize()}\nText:\n{text}"}
        ]
        return self.client.chat(self.summarizer_model, msgs, stream=False).strip()

    def _merge_into_digest(self, existing_digest: str, new_chunk_text: str) -> str:
        """
        Update the rolling digest by merging in new turns (already role-tagged).
        Returns the NEW digest text (not just the delta).
        """
        sys_msg = (
            "You maintain a rolling digest of a long conversation.\n"
            "Update the digest to incorporate the NEW TURNS while preserving durable facts, "
            "decisions, constraints, and important context. Merge—do not duplicate. "
            "If prior details are clarified or corrected, reflect the corrected version. "
            "Keep the digest 4–8 sentences (~200 tokens)."
        )
        msgs = [
            {"role": "system", "content": sys_msg},
            {"role": "user", "content": f"CURRENT DIGEST:\n{existing_digest or '(none)'}"},
            {"role": "user", "content": f"NEW TURNS (chronological, with roles):\n{new_chunk_text}"},
            {"role": "user", "content": "Return ONLY the revised digest text."},
        ]
        return self.client.chat(self.summarizer_model, msgs, stream=False).strip()

    def _ensure_summary(self, idx: int) -> str:
        s = self.turns[idx].get("summary")
        if s:
            return s
        role = self.turns[idx]["role"]
        text = self.turns[idx]["text"]
        s = self._summarize_turn(role, text)
        self.turns[idx]["summary"] = s
        return s

    def _last_full_pairs(self, k_pairs: int) -> List[Tuple[str, str]]:
        pairs: List[Tuple[str, str]] = []
        i = len(self.turns) - 1
        while i >= 1 and len(pairs) < k_pairs:
            a = self.turns[i]
            b = self.turns[i - 1]
            if a["role"] == "assistant" and b["role"] == "user":
                pairs.append((b["text"], a["text"]))
                i -= 2
            else:
                i -= 1
        pairs.reverse()
        return pairs

    def _recent_summaries(self, *, before_pairs: int, limit: int) -> List[str]:
        # find the index where the last full pairs start, then walk backward to collect summaries
        if before_pairs <= 0:
            end = len(self.turns)
        else:
            # compute where those pairs began
            # we reconstruct similarly to _last_full_pairs but only to find the boundary
            boundary = len(self.turns)
            found = 0
            i = len(self.turns) - 1
            stack = []
            while i >= 1 and found < before_pairs:
                a, b = self.turns[i], self.turns[i-1]
                if a["role"] == "assistant" and b["role"] == "user":
                    stack.append((i-1, i))
                    found += 1
                    i -= 2
                else:
                    i -= 1
            if stack:
                boundary = stack[0][0]  # first pair's user index
            end = boundary
        # take up to `limit` single-turn summaries before that boundary
        start = max(0, end - limit)
        out: List[str] = []
        for j in range(start, end):
            out.append(self._ensure_summary(j))
        return out

    def _digest_tail(self) -> Optional[str]:
        return self.digest_text or None