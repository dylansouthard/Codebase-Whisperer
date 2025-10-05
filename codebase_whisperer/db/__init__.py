# codebase_whisperer/db/__init__.py  (re-export from io.py)
from .io import (
    open_db,                       # (db_dir) -> conn
    ensure_chunks,                 # (conn, table_name, embedding_dim) -> tbl
    ensure_vec_cache,              # (conn, embedding_dim) -> tbl
    ensure_vector_index,           # (tbl, metric="cosine") -> None
    try_add_missing_columns,       # (tbl, {name: pa.type}) -> None
    upsert_rows,                   # (tbl, rows, on) -> None
    delete_where,                  # (tbl, where_sql) -> None
    load_vec_cache_map,            # (vcache_tbl, model) -> {sha: [float]}
    validate_vectors,              # (rows, dim) -> None (raise on bad)
    table_counts,                  # (conn) -> {name: int}
    vacuum_table,                  # (tbl) -> None
)
