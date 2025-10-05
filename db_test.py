import lancedb

db = lancedb.connect("./db")
tbl = db.open_table("chunks")

# Convert to pandas and inspect
df = tbl.to_pandas()
print("Row count:", len(df))
print("First rows:\n", df.head())