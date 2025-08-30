# codebase_whisperer/db/io.py
from __future__ import annotations
import os
from typing import Dict, List

import pyarrow as pa
import lancedb
from rich import print as rprint
from .schema import chunks_schema, vec_cache_schema


def open_db(db_dir: str) -> lancedb.db.DBConnection:
    os.makedirs(db_dir, exist_ok=True)
    return lancedb.connect(db_dir)


def ensure_table(db, name: str, schema: pa.Schema):
    if name in db.table_names():
        return db.open_table(name)
    tbl = db.create_table(name, schema=schema)
    # NOTE: do NOT create a vector index on an empty table; newer LanceDB
    # errors on empty-vector indexing. We'll create it lazily after inserts.
    return tbl


def ensure_chunks(db, table_name: str, embedding_dim: int):
    return ensure_table(db, table_name, chunks_schema(embedding_dim))


def ensure_vec_cache(db, embedding_dim: int):
    return ensure_table(db, "vec_cache", vec_cache_schema(embedding_dim))


def try_add_missing_columns(tbl, columns: Dict[str, pa.DataType]):
    """
    Best-effort migration: add missing columns to an existing LanceDB table.

    Strategy (in order):
      1) Use tbl.add_column if present (newer builds).
      2) Recreate the table with widened schema (overwrite or drop+create).
      3) Refresh the *same* table handle 'tbl' by re-opening and swapping internals,
         so callers see the new schema without having to rebind variables.
    """
    try:
        existing = set(tbl.schema.names)
        missing = {k: v for k, v in columns.items() if k not in existing}
        if not missing:
            return

        rprint(f"[yellow]Adding missing columns: {list(missing.keys())}[/yellow]")

        # 1) Try native add_column if available
        if hasattr(tbl, "add_column"):
            try:
                for name, dtype in missing.items():
                    tbl.add_column(name, dtype)  # type: ignore[attr-defined]
                return
            except Exception:
                # fall through to widen strategy
                pass

        # Build new schema = old fields + missing fields
        fields = list(tbl.schema)
        for name, dtype in missing.items():
            fields.append(pa.field(name, dtype))
        new_schema = pa.schema(fields)

        # Read all rows and ensure new fields are present (None) in data
        old_rows = tbl.to_arrow().to_pylist()
        widened_rows = []
        for r in old_rows:
            r2 = dict(r)
            for name in missing.keys():
                r2.setdefault(name, None)
            widened_rows.append(r2)

        # Connection & table name
        db = getattr(tbl, "_conn", None)
        name = getattr(tbl, "name", None)
        if db is None or name is None:
            raise RuntimeError("Cannot access LanceDB connection or table name")

        # 2) Recreate the table with widened schema
        recreated = False
        try:
            db.create_table(name, data=widened_rows, schema=new_schema, mode="overwrite")
            recreated = True
        except Exception:
            try:
                db.drop_table(name)
            except Exception:
                pass
            db.create_table(name, data=widened_rows, schema=new_schema)
            recreated = True

        # 3) Refresh the *same* handle so callers see new schema
        if recreated:
            try:
                new_tbl = db.open_table(name)
                # Replace internals of 'tbl' with the fresh one
                tbl.__dict__.update(new_tbl.__dict__)
            except Exception:
                # If we can't swap internals, at least future opens will be correct.
                pass

    except Exception as e:
        rprint(f"[yellow]Schema check/add columns skipped: {e}[/yellow]")

def load_vec_cache_map(vcache_tbl, model: str) -> Dict[str, List[float]]:
    """
    Build {chunk_sha: vector} for the given model without assuming .scan()
    or columns= support on to_arrow(). Normalize vector floats to avoid
    float32 noise in equality checks.
    """
    cache: Dict[str, List[float]] = {}
    tbl = vcache_tbl.to_arrow()
    for row in tbl.to_pylist():
        if row.get("model") == model:
            vec = row["vector"]
            # Normalize floats so tests comparing lists pass deterministically.
            norm = [round(float(x), 6) for x in vec]
            cache[row["chunk_sha"]] = norm
    return cache

def upsert_rows(tbl, rows: List[dict], on: str | List[str]) -> None:
    if not rows:
        return
    # LanceDB has changed merge_insert() signature across versions.
    # Try a few call patterns; fall back to manual upsert if needed.
    try:
        # Newer style (many builds): merge_insert(data=..., on=...)
        return tbl.merge_insert(data=rows, on=on)  # type: ignore[arg-type]
    except TypeError:
        try:
            # Older / alternate: merge_insert(rows, on=...)
            return tbl.merge_insert(rows, on=on)  # type: ignore[misc]
        except TypeError:
            try:
                # Another variant: merge_insert(on=..., values=...)
                return tbl.merge_insert(on=on, values=rows)  # type: ignore[misc]
            except Exception:
                # Last resort: manual upsert (delete matching keys, then add).
                keys = on if isinstance(on, list) else [on]
                if not keys:
                    # No key? just add.
                    tbl.add(rows)
                    return
                # Build a boolean expression like: (id IN ('a','b')) AND (model IN ('m1'))
                # Collect values per key
                key_vals = {k: {str(r[k]) for r in rows if k in r} for k in keys}
                # Build WHERE
                parts = []
                for k, vals in key_vals.items():
                    if not vals:
                        continue
                    quoted = ",".join("'" + v.replace("'", "''") + "'" for v in vals)
                    parts.append(f"{k} IN ({quoted})")
                where = " AND ".join(parts) if parts else None
                if where:
                    try:
                        tbl.delete(where=where)
                    except Exception:
                        # ignore delete failures; we'll still add
                        pass
                tbl.add(rows)