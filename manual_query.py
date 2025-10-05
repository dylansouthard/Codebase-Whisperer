from __future__ import annotations
import argparse
from rich import print as rprint

from codebase_whisperer.pipelines.query import query_repo, chat_with_context

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--question", required=True, help="Natural language question")
    ap.add_argument("--db-dir", default=None)
    ap.add_argument("--table-name", default=None)
    ap.add_argument("--config", default=None, help="Optional path to config.yaml/json")
    ap.add_argument("--top-k", type=int, default=None)
    args = ap.parse_args()

    hits = query_repo(
        args.question,
        db_dir=args.db_dir,
        table_name=args.table_name,
        config_path=args.config,
        top_k=args.top_k,
    )
    rprint(f"[cyan]Top {len(hits)} matches:[/cyan]")
    for i, h in enumerate(hits, 1):
        rprint(f"[bold]{i}.[/bold] {h.get('relpath','?')}:{h.get('chunk_idx','?')}  score={h.get('_distance','?')}")
        rprint(h.get("content","")[:300] + ("..." if len(h.get("content",""))>300 else ""))
        rprint()

    answer = chat_with_context(
        args.question,
        hits,
        config_path=args.config,
    )
    rprint("\n[green]Answer:[/green]\n" + answer)

if __name__ == "__main__":
    main()