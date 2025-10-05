# codebase_whisperer/llm/session.py
from __future__ import annotations
from typing import Optional, List, Dict, Any, Callable
from dataclasses import dataclass

from codebase_whisperer.llm.memory import Memory, MemoryConfig
from .ollama import OllamaClient
from codebase_whisperer.config import load_config
from ..pipelines.query import query_repo

@dataclass
class SessionConfig:
    host:str
    chat_model: str
    embed_model: str
    top_k: int
    max_context_chars: int
    chat_context:Optional[int] = None
    memory_cfg:Optional[MemoryConfig] = None
    pinned: Optional[List[str]] = None

class OllamaChatSession:
    def __init__(self, client:OllamaClient, cfg:SessionConfig):

        self.client = client
        self.cfg = cfg

        self.memory = Memory(
            client=self.client,
            chat_model=self.cfg.chat_model,
            embed_model=self.cfg.embed_model,
            pinned=self.cfg.pinned or [],
            cfg=self.cfg.memory_cfg or MemoryConfig()
        )

    @classmethod
    def from_config(cls, config_path: Optional[str] = None) -> OllamaChatSession:
        cfg_dict, _ = load_config(config_path)

        host = cfg_dict.get("ollama", {}).get("host", "http://localhost:11434")
        chat_model  = cfg_dict.get("ollama", {}).get("chat_model", "llama3:8b")
        chat_ctx = cfg_dict.get("ollama", {}).get("chat_context")  # optional

        embed_model = cfg_dict.get("embedding", {}).get("model", "nomic-embed-text")

        top_k = int(cfg_dict.get("retrieval", {}).get("top_k", 5))
        max_context_chars= int(cfg_dict.get("retrieval", {}).get("max_context_chars", 14000))

        mem_section = cfg_dict.get("memory", {}) or {}
        mem_cfg = MemoryConfig(
            max_full_pairs = int(mem_section.get("max_full_pairs", 2)),
            max_recent_summaries = int(mem_section.get("max_recent_summaries", 2)),
            summarize_every = int(mem_section.get("summarize_every", 6)),
            max_turns = int(mem_section.get("max_turns", 200)),
            digest_max_chars = int(mem_section.get("digest_max_chars", 12_000)),
            summarizer_model = mem_section.get("summarizer_model") or chat_model,
            embed_turns = bool(mem_section.get("embed_turns", False)),
        )
        pinned = mem_section.get("pinned") or []

        client = OllamaClient(host)

        return cls(
            client = client,
            cfg=SessionConfig(
                host=host,
                chat_model=chat_model,
                embed_model=embed_model,
                top_k=top_k,
                max_context_chars=max_context_chars,
                chat_context=chat_ctx,
                memory_cfg=mem_cfg,
                pinned=pinned
            ),
        )
    # --- core turn handler (RAG + memory) -----------------------------------
    def ask(
            self,
            question: str,
            *,
            db_dir:str,
            table_name:str,
            stream: bool = True,
            on_chunk: Optional[Callable[[str], None]] = None
    ) -> str:
        """
        1) Build memory context text
        2) RAG retrieve chunks
        3) Compose messages & call LLM (stream or not)
        4) Save assistant turn back to memory
        """
        # 1) user turn -> memory
        # self.memory.append("user", question)

        # 1) memory context text
        mem_block = self.memory.as_text_block(question)

        # 2) retrieve
        hits = query_repo(
            question=question,
            db_dir=db_dir,
            table_name=table_name,
            host=self.cfg.host,
            embed_model=self.cfg.embed_model,
            top_k=self.cfg.top_k,
        )
        # pack RAG context (respect the retrieval char cap)
        pieces: List[str] = []
        total = 0
        for h in hits:
            snippet = f"{h['relpath']}:{h['chunk_idx']}\n{h['content']}\n"
            if total + len(snippet) > self.cfg.max_context_chars:
                break
            pieces.append(snippet)
            total += len(snippet)
        rag_ctx = "\n\n".join(pieces)

        # 3) compose messages
        messages = [
            {
                "role": "system",
                "content": (
                    "You are a coding assistant. Use ONLY the provided RAG context and memory when answering. "
                    "If the answer is not supported by context/memory, say so explicitly."
                ),
            },
            {
                "role": "user",
                "content": (
                    f"Memory (rolling digest + recent turns):\n{mem_block or '(none)'}\n\n"
                    f"RAG Context:\n{rag_ctx or '(none)'}\n\n"
                    f"Question: {question}"
                ),
            },
        ]

        # 4) chat
        if stream:
            chunks: List[str] = []
            def _on_chunk(s: str):
                chunks.append(s)
                if on_chunk:
                    on_chunk(s)

            answer = self.client.chat(self.cfg.chat_model, messages, stream=True, on_chunk=_on_chunk)
            final = answer or "".join(chunks)
        else:
            final = self.client.chat(self.cfg.chat_model, messages, stream=False)

        # Append as a single complete pair to avoid double-triggerSing absorb
        self.memory.append_pair(question, final.strip())

        return final.strip()
