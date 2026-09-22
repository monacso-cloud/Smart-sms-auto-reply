import { getPool } from "./db.js";

export async function purgeExpiredCallLogs() {
  const db = getPool();
  if (!db) return { deleted: 0 };

  const result = await db.query(`
    DELETE FROM replydesk_call_logs l
    USING replydesk_reply_settings s
    WHERE l.business_id = s.business_id
      AND s.auto_delete_logs = TRUE
      AND l.occurred_at < NOW() - (s.call_log_retention_days || ' days')::interval
  `);

  return { deleted: result.rowCount || 0 };
}
