# manual_ingest.py
import argparse
from codebase_whisperer.pipelines.ingest import run_ingest

def main():
    parser = argparse.ArgumentParser(description="Manually run ingest pipeline")
    parser.add_argument("--repo-root", required=True, help="Path to repository to ingest")
    parser.add_argument("--db-dir", required=True, help="Path to database directory")
    parser.add_argument("--table-name", required=True, help="Target table name")
    parser.add_argument("--config-path", help="Optional path to config file")
    parser.add_argument("--force-reembed", action="store_true", help="Force re-embedding even if cache exists")

    args = parser.parse_args()

    run_ingest(
        repo_root=args.repo_root,
        db_dir=args.db_dir,
        table_name=args.table_name,
        config_path=args.config_path,
        force_reembed=args.force_reembed,
    )

if __name__ == "__main__":
    main()