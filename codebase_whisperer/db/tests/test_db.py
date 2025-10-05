# tests/test_db_io.py
import pyarrow as pa
import lancedb
from pathlib import Path

from ..io import (
    open_db,
    ensure_chunks,
    ensure_vec_cache,
    try_add_missing_columns,
    load_vec_cache_map,
    upsert_rows,
)
from ..schema import chunks_schema, vec_cache_schema


def test_open_db_creates_dir_and_connects(tmp_path: Path):
    db_dir = tmp_path / "ldb"
    assert not db_dir.exists()
    db = open_db(str(db_dir))
    assert db_dir.exists()
    assert isinstance(db, lancedb.db.DBConnection)


def test_ensure_chunks_and_vec_cache_tables_exist(tmp_path: Path):
    db = open_db(str(tmp_path / "ldb"))
    chunks = ensure_chunks(db, "chunks", embedding_dim=8)
    vcache = ensure_vec_cache(db, embedding_dim=8)

    # basic existence
    assert "chunks" in db.table_names()
    assert "vec_cache" in db.table_names()

    # schema sanity checks
    ch_schema = chunks.schema
    vc_schema = vcache.schema
    for name in [
        "id","path","realpath","is_symlink","relpath","lang","symbol",
        "chunk_idx","content","sha256","content_sha","mtime","vector"
    ]:
        assert name in ch_schema.names
    for name in ["chunk_sha","model","vector"]:
        assert name in vc_schema.names

    # smoke: vector column is list<float32>
    ch_vector_field = ch_schema.field("vector")
    assert pa.types.is_list(ch_vector_field.type)
    assert pa.types.is_float32(ch_vector_field.type.value_type)


def test_try_add_missing_columns_adds_new_fields(tmp_path: Path):
    db = open_db(str(tmp_path / "ldb"))

    # Create a minimal table missing a couple of columns (directly via LanceDB)
    minimal_schema = pa.schema([
        ("id", pa.string()),
        ("vector", pa.list_(pa.float32())),   # generic list<float32>
    ])
    tbl = db.create_table("minichunks", schema=minimal_schema)

    # Add columns via our helper
    try_add_missing_columns(tbl, {
        "path": pa.string(),
        "mtime": pa.float64(),
    })

    names = set(tbl.schema.names)
    assert "path" in names
    assert "mtime" in names


def test_upsert_rows_inserts_and_merges(tmp_path: Path):
    db = open_db(str(tmp_path / "ldb"))

    # Create simple key/value table (directly via LanceDB)
    tbl = db.create_table("rows", schema=pa.schema([
        ("id", pa.string()),
        ("value", pa.int32()),
    ]))

    # Insert two rows
    upsert_rows(tbl, [{"id":"a","value":1}, {"id":"b","value":2}], on="id")
    df1 = tbl.to_pandas()
    assert set(df1["id"]) == {"a","b"}
    assert dict(zip(df1["id"], df1["value"])) == {"a":1,"b":2}

    # Upsert with same id changes value
    upsert_rows(tbl, [{"id":"a","value":10}], on="id")
    df2 = tbl.to_pandas()
    assert set(df2["id"]) == {"a","b"}
    assert dict(zip(df2["id"], df2["value"])) == {"a":10,"b":2}


def test_load_vec_cache_map_filters_by_model(tmp_path: Path):
    db = open_db(str(tmp_path / "ldb"))
    vcache = ensure_vec_cache(db, embedding_dim=4)

    # Insert different models
    rows = [
        {"chunk_sha":"X", "model":"m1", "vector":[0.1,0.2,0.3,0.4]},
        {"chunk_sha":"Y", "model":"m2", "vector":[0.9,0.8,0.7,0.6]},
        {"chunk_sha":"Z", "model":"m1", "vector":[0.0,0.0,1.0,1.0]},
    ]
    upsert_rows(vcache, rows, on=["chunk_sha","model"])

    cache_m1 = load_vec_cache_map(vcache, "m1")
    cache_m2 = load_vec_cache_map(vcache, "m2")

    assert set(cache_m1.keys()) == {"X","Z"}
    assert set(cache_m2.keys()) == {"Y"}
    assert cache_m1["X"] == [0.1,0.2,0.3,0.4]
    assert cache_m2["Y"] == [0.9,0.8,0.7,0.6]