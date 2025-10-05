# rag_cli.py
# import argparse, os, sys
# from rich import print
# from codebase_whisperer.pipelines.query import query_repo
# from codebase_whisperer.config import load_config
# from codebase_whisperer.llm.ollama import OllamaClient
# from codebase_whisperer.reasoning.iterative import iterative_answer


# def main():
#     ap = argparse.ArgumentParser()
#     ap.add_argument("--db-dir", default="./db")
#     ap.add_argument("--table-name", default="chunks")
#     ap.add_argument("--top-k", type=int, default=5)
#     ap.add_argument("--no-stream", action="store_true", help="Disable streaming output")
#     args = ap.parse_args()

#     cfg, _ = load_config(None)
#     host = cfg.get("ollama", {}).get("host", "http://localhost:11434")
#     chat_model = cfg.get("ollama", {}).get("chat_model", "llama3:8b")
#     embed_model = cfg.get("embedding", {}).get("model", "nomic-embed-text")

#     client = OllamaClient(host)

#     print("[bold green]RAG CLI ready. Type your question. (:q to quit)[/bold green]")
#     while True:
#         try:
#             q = input("\n[you] ").strip()
#         except (EOFError, KeyboardInterrupt):
#             print("\nbye!")
#             break
#         if not q or q == ":q":
#             break

#         hits = query_repo(
#             question=q,
#             db_dir=args.db_dir,
#             table_name=args.table_name,
#             host=host,
#             embed_model=embed_model,
#             top_k=args.top_k,
#         )

#         # pack context
#         ctx = "\n\n".join(f"{h['relpath']}:{h['chunk_idx']}\n{h['content']}" for h in hits)
#         messages = [
#             {"role":"system","content":"You are a coding assistant. Use ONLY the provided context. If the answer isn’t in the context, say so."},
#             {"role":"user","content": f"Context:\n{ctx}\n\nQuestion: {q}"}
#         ]

#         print("\n[bold]Answer:[/bold]\n", end="", flush=True)

#         if args.no_stream:
#             # non-stream mode
#             ans = client.chat(chat_model, messages, stream=False)
#             print(ans)
#         else:
#             # STREAMING MODE
#             def on_chunk(s: str):
#                 # print tokens as they arrive, no extra spaces/newlines
#                 print(s, end="", flush=True)

#             try:
#                 # chat() will still return the final text; we don't actually need it here
#                 client.chat(chat_model, messages, stream=True, on_chunk=on_chunk)
#             except KeyboardInterrupt:
#                 # let user stop generation mid-way
#                 print("\n[dim]stopped[/dim]")
#             else:
#                 # ensure we end on a newline after the last chunk
#                 print()
#         # if args.no_stream:
#         #     out = iterative_answer(
#         #         client=client,
#         #         model=chat_model,
#         #         user_question=q,
#         #         context_blocks=[ctx],
#         #         beams=1,  # try 3 for tougher questions
#         #         stream_final=False,
#         #     )
#         #     print(out)
#         # else:
#         #     def on_chunk(s: str):
#         #         print(s, end="", flush=True)

#         #     iterative_answer(
#         #         client=client,
#         #         model=chat_model,
#         #         user_question=q,
#         #         context_blocks=[ctx],
#         #         beams=1,
#         #         stream_final=True,
#         #         on_final_chunk=on_chunk,
#         #     )
#         #     print()
# if __name__ == "__main__":
#     main()
# rag_cli.py
import argparse
from rich import print
from codebase_whisperer.llm.session import OllamaChatSession

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db-dir", default="./db")
    ap.add_argument("--table-name", default="chunks")
    ap.add_argument("--no-stream", action="store_true")
    args = ap.parse_args()

    session = OllamaChatSession.from_config(None)

    print("[bold green]RAG CLI ready. Type your question. (:q to quit)[/bold green]")
    while True:
        try:
            q = input("\n[you] ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nbye!")
            break
        if not q or q == ":q":
            break

        print("\n[bold]Answer:[/bold]\n", end="", flush=True)
        if args.no_stream:
            ans = session.ask(q, db_dir=args.db_dir, table_name=args.table_name, stream=False)
            print(ans)
        else:
            def on_tok(s: str):
                print(s, end="", flush=True)
            session.ask(q, db_dir=args.db_dir, table_name=args.table_name, stream=True, on_chunk=on_tok)
            print()  # newline after stream

if __name__ == "__main__":
    main()