import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getPool } from "./db.js";

const db = getPool();
if (!db) throw new Error("DATABASE_URL is required");

const here = path.dirname(fileURLToPath(import.meta.url));
const dir = path.resolve(here, "../sql");
const files = (await fs.readdir(dir)).filter(name => name.endsWith(".sql")).sort();

await db.query(`
  CREATE TABLE IF NOT EXISTS replydesk_schema_migrations (
    filename TEXT PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  )
`);

for (const filename of files) {
  const existing = await db.query(
    "SELECT 1 FROM replydesk_schema_migrations WHERE filename = $1",
    [filename]
  );
  if (existing.rowCount) continue;

  const sql = await fs.readFile(path.join(dir, filename), "utf8");
  const client = await db.connect();
  try {
    await client.query("BEGIN");
    await client.query(sql);
    await client.query(
      "INSERT INTO replydesk_schema_migrations(filename) VALUES($1)",
      [filename]
    );
    await client.query("COMMIT");
    console.log("Applied", filename);
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

await db.end();
