import pg from "pg";

const { Pool } = pg;

let pool;

export function getPool() {
  if (!process.env.DATABASE_URL) return null;
  if (!pool) {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: process.env.NODE_ENV === "production" ? { rejectUnauthorized: false } : undefined,
    });
  }
  return pool;
}

export async function checkDatabase() {
  const db = getPool();
  if (!db) return { configured: false, reachable: false };
  try {
    await db.query("select 1");
    return { configured: true, reachable: true };
  } catch {
    return { configured: true, reachable: false };
  }
}
