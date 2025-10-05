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


def _ensure_table(db, name: str, schema: pa.Schema):
    if name in db.table_names():
        return db.open_table(name)
    tbl = db.create_table(name, schema=schema)
    # NOTE: do NOT create a vector index on an empty table; newer LanceDB
    # errors on empty-vector indexing. We'll create it lazily after inserts.
    return tbl


def ensure_chunks(db, table_name: str, embedding_dim: int):
    return _ensure_table(db, table_name, chunks_schema(embedding_dim))


def ensure_vec_cache(db, embedding_dim: int):
    return _ensure_table(db, "vec_cache", vec_cache_schema(embedding_dim))


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

# ---- extras: safe, optional helpers for DB black box -----------------
from typing import Iterable, Optional

def ensure_vector_index(tbl, *, column: str = "vector", metric: str = "cosine") -> None:
    """
    Lazily create a vector index on `column` using `metric`.
    - Skips if table is empty (many LanceDB builds error on empty indexing)
    - Skips if index already exists (best-effort detection)
    - Tolerates API drift across LanceDB versions
    """
    try:
        # no rows? bail
        try:
            n = getattr(tbl, "count_rows", lambda: tbl.to_arrow().num_rows)()
            if n == 0:
                return
        except Exception:
            # last resort: pull one row
            arr = tbl.to_arrow()
            if getattr(arr, "num_rows", 0) == 0:
                return

        # already indexed? best effort check
        try:
            idx_info = getattr(tbl, "list_indices", lambda: [])()
            # newer APIs return a list of dicts or objects; check column name
            for it in idx_info or []:
                name = getattr(it, "column", None) or getattr(it, "name", None) or it
                if isinstance(name, dict):
                    name = name.get("column") or name.get("name")
                if name == column:
                    return
        except Exception:
            pass

        # try several create_index signatures
        for attempt in (
            lambda: tbl.create_index(column=column, metric=metric),
            lambda: tbl.create_index(metric=metric, column=column),
            lambda: tbl.create_index(column=column),   # some versions default to cosine
        ):
            try:
                attempt()
                return
            except Exception:
                continue
    except Exception:
        # indexing is best-effort; never fail pipeline because of it
        return


def delete_where(tbl, where_sql: str) -> None:
    """
    Thin wrapper so callers never touch Lance internals directly.
    Accepts a SQL-ish predicate string (e.g., "id IN ('a','b') AND model='m'").
    """
    if not where_sql:
        return
    try:
        tbl.delete(where=where_sql)
    except Exception:
        # Older builds (rare): .delete may use a different kw or not exist.
        # As a last resort, ignore; callers typically follow with adds/upserts.
        return


def validate_vectors(rows: Iterable[dict], dim: int, *, key: str = "vector") -> list[dict]:
    """
    Ensure vectors are present & correct length. Coerce to float and normalize precision.
    Returns a NEW list of rows (does not mutate input).
    Raises ValueError if a vector is wrong length.
    """
    out: list[dict] = []
    for r in rows:
        v = r.get(key)
        if v is None:
            out.append(dict(r))  # allow null; upstream may fill later
            continue
        # accept any iterable of numbers, coerce to list[float]
        vec = [float(x) for x in v]
        if len(vec) != dim:
            raise ValueError(f"vector length {len(vec)} != expected {dim}")
        # normalize to float32 precision for stable equality in tests
        vec = [round(float(x), 6) for x in vec]
        nr = dict(r)
        nr[key] = vec
        out.append(nr)
    return out


def table_counts(tbl) -> dict:
    """
    Return a small ops summary: {"rows": int}
    """
    try:
        return {"rows": int(getattr(tbl, "count_rows", lambda: tbl.to_arrow().num_rows)())}
    except Exception:
        try:
            return {"rows": int(tbl.to_arrow().num_rows)}
        except Exception:
            return {"rows": -1}


def vacuum_table(tbl) -> None:
    """
    Best-effort maintenance. Different LanceDB versions expose different names.
    No-op on failure.
    """
    for fn_name in ("optimize", "optimize_compaction", "compact_files", "vacuum"):
        fn = getattr(tbl, fn_name, None)
        if callable(fn):
            try:
                fn()
            except Exception:
                pass