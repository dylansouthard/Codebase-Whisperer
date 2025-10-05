import lancedb, pyarrow as pa
db = lancedb.connect("./db")
t = db.open_table("chunks")
print(t.schema)              # shows each field
print("vector field:", t.schema.field("vector").type)