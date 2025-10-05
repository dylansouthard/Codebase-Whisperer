# codebase_whisperer/pipelines/query.py
from __future__ import annotations
from typing import List, Optional
import lancedb

from codebase_whisperer.config import load_config
from codebase_whisperer.llm.ollama import OllamaClient
# --- debug helper ---
import os, sys, json
def _dbg(*a, **kw):
    if os.environ.get("RAG_DEBUG") == "1":
        print(*a, file=sys.stderr, **kw)

def query_repo(
    question: str,
    *,
    db_dir: Optional[str] = None,
    table_name: Optional[str] = None,
    config_path: Optional[str] = None,
    # optional one-off overrides; if None we use config values
    embed_model: Optional[str] = None,
    host: Optional[str] = None,
    top_k: Optional[int] = None,
) -> List[dict]:
    """
    1) Read config
    2) Embed question (Ollama)
    3) Search LanceDB for similar chunks
    Returns: list[dict] records for easy downstream use.
    """
    cfg, _ = load_config(config_path)
    _dbg("Loaded config:", json.dumps(cfg, indent=2))

    db_dir = db_dir or cfg["db_dir"]
    table_name = table_name or cfg["table"]
    model = embed_model or cfg["embedding"]["model"]
    host = host or cfg["ollama"]["host"]
    top_k = top_k or cfg["retrieval"]["top_k"]
    _dbg(f"db_dir={db_dir}, table_name={table_name}, model={model}, host={host}, top_k={top_k}")


    # Connect to DB/table
    db = lancedb.connect(db_dir)
    table = db.open_table(table_name)
    _dbg("Opened table schema:", table.schema)

    # Embed question
    client = OllamaClient(host)
    [query_vec] = client.embed(model, [question])
    _dbg(f"Question='{question}' -> embedding len={len(query_vec)}")
    # Vector search
    q = (
        table.search(query_vec, vector_column_name="vector")
             .metric("cosine")
             .limit(top_k)
             # select only the fields we need (optional but keeps payload small)
             .select(["id", "relpath", "chunk_idx", "content", "_distance"])
    )

    # Convert to list of dicts so callers can do row.get(...)
    df = q.to_pandas()
    _dbg("Search results:", df.head().to_dict(orient="records"))
    return df.to_dict(orient="records")


def chat_with_context(
    question: str,
    context_chunks: List[dict],
    *,
    config_path: Optional[str] = None,
    chat_model: Optional[str] = None,
    host: Optional[str] = None,
) -> str:
    """
    Use retrieved context + Llama chat to answer the question.
    Reads chat model/host from config unless overridden.
    """
    cfg, _ = load_config(config_path)
    host = host or cfg["ollama"]["host"]
    chat_model = chat_model or cfg["ollama"]["chat_model"]
    _dbg(f"Using chat_model={chat_model} host={host}")

    # Join content; keep prompt bounded by max_context_chars
    max_ctx = int(cfg["retrieval"]["max_context_chars"])
    context_text = ""
    for row in context_chunks:
        piece = row.get("content", "")
        _dbg(f"Adding chunk: id={row.get('id')} len={len(piece)}")
        if len(context_text) + len(piece) + 2 > max_ctx:
            _dbg("Context length limit reached, truncating")
            break
        context_text += (("\n\n" if context_text else "") + piece)

    _dbg("Final context length:", len(context_text))
    messages = [
        {"role": "system", "content": "You are a coding assistant. Use ONLY the provided context when answering. If the answer isn't in the context, say so."},
        {"role": "user", "content": f"Context:\n{context_text}\n\nQuestion: {question}"},
    ]
    _dbg("Messages sent to chat model:", json.dumps(messages, indent=2)[:500])
    client = OllamaClient(host)
    resp = client.chat(chat_model, messages)
    _dbg("Chat response:", resp)
    return resp