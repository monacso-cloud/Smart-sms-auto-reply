import { Router } from "express";
import { getPool } from "./db.js";
import { requireAuth } from "./auth.js";
import { normalizeText, selectKeywordMatch } from "./keywordMatcher.js";
import { purgeExpiredCallLogs } from "./callLogRetention.js";

export const api = Router();

api.use(requireAuth);

api.get("/me", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const result = await db.query(
    "SELECT id, email, display_name, status, created_at FROM replydesk_accounts WHERE id = $1",
    [req.auth.accountId]
  );
  if (!result.rowCount) return res.status(404).json({ error: "account_not_found" });
  res.json({ account: result.rows[0] });
});

api.get("/subscription", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const result = await db.query(
    `SELECT platform, external_product_id, status, trial_ends_at,
            current_period_ends_at, auto_renews, updated_at
       FROM replydesk_subscription_entitlements
      WHERE account_id = $1
      ORDER BY updated_at DESC
      LIMIT 1`,
    [req.auth.accountId]
  );

  res.json({
    entitlement: result.rows[0] || {
      status: "expired",
      auto_renews: false,
    },
  });
});

api.get("/businesses", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });
  const result = await db.query(
    "SELECT id, name, timezone, created_at FROM replydesk_businesses WHERE owner_account_id = $1 ORDER BY id",
    [req.auth.accountId]
  );
  res.json({ businesses: result.rows });
});

api.get("/businesses/:businessId/rules", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const result = await db.query(
    `SELECT id, enabled, trigger_type, start_local_time, end_local_time,
            reply_mode, message_template, created_at, updated_at
       FROM replydesk_automation_rules
      WHERE business_id = $1
      ORDER BY id`,
    [req.params.businessId]
  );
  res.json({ rules: result.rows });
});

api.get("/businesses/:businessId/handovers", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const result = await db.query(
    `SELECT h.id, h.department, h.assigned_staff_id, h.status,
            h.accepted_at, h.closed_at, h.created_at
       FROM replydesk_handovers h
      WHERE h.business_id = $1
      ORDER BY h.created_at DESC
      LIMIT 100`,
    [req.params.businessId]
  );
  res.json({ handovers: result.rows });
});

api.post("/handovers/:handoverId/accept", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });
  const staffId = Number(req.body?.staffId);
  if (!Number.isInteger(staffId) || staffId <= 0) {
    return res.status(400).json({ error: "invalid_staff_id" });
  }

  const result = await db.query(
    `UPDATE replydesk_handovers h
        SET assigned_staff_id = $1,
            status = 'accepted',
            accepted_at = COALESCE(accepted_at, NOW())
       FROM replydesk_staff_members s,
            replydesk_businesses b
      WHERE h.id = $2
        AND s.id = $1
        AND s.business_id = h.business_id
        AND b.id = h.business_id
        AND b.owner_account_id = $3
        AND h.status = 'open'
      RETURNING h.id, h.status, h.assigned_staff_id, h.accepted_at`,
    [staffId, req.params.handoverId, req.auth.accountId]
  );
  if (!result.rowCount) return res.status(409).json({ error: "handover_not_available" });
  res.json({ handover: result.rows[0] });
});

api.delete("/account", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const businesses = await db.query(
    "SELECT id FROM replydesk_businesses WHERE owner_account_id = $1",
    [req.auth.accountId]
  );
  if (businesses.rowCount) {
    return res.status(409).json({
      error: "owned_business_exists",
      message: "Transfer or delete owned business data before deleting this account.",
    });
  }

  await db.query("DELETE FROM replydesk_accounts WHERE id = $1", [req.auth.accountId]);
  res.status(204).end();
});


api.get("/businesses/:businessId/reply-settings", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const settings = await db.query(
    "SELECT * FROM replydesk_reply_settings WHERE business_id = $1",
    [req.params.businessId]
  );
  const windows = await db.query(
    `SELECT id, enabled, day_of_week, start_local_time, end_local_time
       FROM replydesk_schedule_windows
      WHERE business_id = $1
      ORDER BY day_of_week, start_local_time`,
    [req.params.businessId]
  );

  res.json({
    settings: settings.rows[0] || {
      business_id: Number(req.params.businessId),
      master_enabled: false,
      reply_to_missed_calls: true,
      reply_to_incoming_sms: false,
      schedule_mode: "always",
      timezone: "Australia/Perth",
      call_log_retention_days: 14,
      auto_delete_logs: true,
    },
    scheduleWindows: windows.rows,
  });
});

api.put("/businesses/:businessId/reply-settings", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const {
    masterEnabled,
    replyToMissedCalls,
    replyToIncomingSms,
    scheduleMode,
    timezone,
    callLogRetentionDays,
    autoDeleteLogs,
  } = req.body || {};

  const retention = Number(callLogRetentionDays);
  if (![7, 14, 30].includes(retention)) {
    return res.status(400).json({ error: "invalid_retention_days" });
  }

  const validModes = new Set(["always", "custom_hours", "off"]);
  if (!validModes.has(scheduleMode)) {
    return res.status(400).json({ error: "invalid_schedule_mode" });
  }

  const result = await db.query(
    `INSERT INTO replydesk_reply_settings
       (business_id, master_enabled, reply_to_missed_calls, reply_to_incoming_sms,
        schedule_mode, timezone, call_log_retention_days, auto_delete_logs, updated_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,NOW())
     ON CONFLICT (business_id)
     DO UPDATE SET
       master_enabled = EXCLUDED.master_enabled,
       reply_to_missed_calls = EXCLUDED.reply_to_missed_calls,
       reply_to_incoming_sms = EXCLUDED.reply_to_incoming_sms,
       schedule_mode = EXCLUDED.schedule_mode,
       timezone = EXCLUDED.timezone,
       call_log_retention_days = EXCLUDED.call_log_retention_days,
       auto_delete_logs = EXCLUDED.auto_delete_logs,
       updated_at = NOW()
     RETURNING *`,
    [
      req.params.businessId,
      Boolean(masterEnabled),
      Boolean(replyToMissedCalls),
      Boolean(replyToIncomingSms),
      scheduleMode,
      timezone || "Australia/Perth",
      retention,
      Boolean(autoDeleteLogs),
    ]
  );

  res.json({ settings: result.rows[0] });
});

api.put("/businesses/:businessId/schedule-windows", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const windows = Array.isArray(req.body?.windows) ? req.body.windows : [];
  for (const item of windows) {
    if (!Number.isInteger(item.dayOfWeek) || item.dayOfWeek < 0 || item.dayOfWeek > 6 ||
        typeof item.startLocalTime !== "string" || typeof item.endLocalTime !== "string") {
      return res.status(400).json({ error: "invalid_schedule_window" });
    }
  }

  const client = await db.connect();
  try {
    await client.query("BEGIN");
    await client.query("DELETE FROM replydesk_schedule_windows WHERE business_id = $1", [req.params.businessId]);
    for (const item of windows) {
      await client.query(
        `INSERT INTO replydesk_schedule_windows
         (business_id, enabled, day_of_week, start_local_time, end_local_time)
         VALUES ($1,$2,$3,$4,$5)`,
        [req.params.businessId, item.enabled !== false, item.dayOfWeek, item.startLocalTime, item.endLocalTime]
      );
    }
    await client.query("COMMIT");
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }

  res.json({ windows });
});

api.get("/businesses/:businessId/keyword-groups", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const groups = await db.query(
    `SELECT g.id, g.name, g.enabled, g.priority, g.match_mode, g.response_text,
            COALESCE(json_agg(k.keyword ORDER BY k.keyword) FILTER (WHERE k.id IS NOT NULL), '[]') AS keywords
       FROM replydesk_keyword_groups g
       LEFT JOIN replydesk_keywords k ON k.group_id = g.id
      WHERE g.business_id = $1
      GROUP BY g.id
      ORDER BY g.priority, g.id`,
    [req.params.businessId]
  );

  res.json({ groups: groups.rows });
});

api.post("/businesses/:businessId/keyword-groups", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const {
    name,
    enabled = true,
    priority = 100,
    matchMode = "contains",
    responseText,
    keywords = [],
  } = req.body || {};

  if (!name || !responseText || !Array.isArray(keywords) || keywords.length === 0) {
    return res.status(400).json({ error: "invalid_keyword_group" });
  }
  if (!["contains", "exact", "starts_with"].includes(matchMode)) {
    return res.status(400).json({ error: "invalid_match_mode" });
  }

  const client = await db.connect();
  try {
    await client.query("BEGIN");
    const group = await client.query(
      `INSERT INTO replydesk_keyword_groups
       (business_id, name, enabled, priority, match_mode, response_text)
       VALUES ($1,$2,$3,$4,$5,$6)
       RETURNING *`,
      [req.params.businessId, name, Boolean(enabled), Number(priority) || 100, matchMode, responseText]
    );

    for (const keyword of keywords) {
      const normalized = normalizeText(keyword);
      if (!normalized) continue;
      await client.query(
        `INSERT INTO replydesk_keywords(group_id, keyword, normalized_keyword)
         VALUES ($1,$2,$3)
         ON CONFLICT (group_id, normalized_keyword) DO NOTHING`,
        [group.rows[0].id, String(keyword).trim(), normalized]
      );
    }

    await client.query("COMMIT");
    res.status(201).json({ group: group.rows[0] });
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
});

api.post("/businesses/:businessId/test-match", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const message = String(req.body?.message || "");
  const data = await db.query(
    `SELECT g.id, g.name, g.enabled, g.priority, g.match_mode, g.response_text,
            COALESCE(json_agg(k.keyword ORDER BY length(k.normalized_keyword) DESC)
              FILTER (WHERE k.id IS NOT NULL), '[]') AS keywords
       FROM replydesk_keyword_groups g
       LEFT JOIN replydesk_keywords k ON k.group_id = g.id
      WHERE g.business_id = $1
      GROUP BY g.id`,
    [req.params.businessId]
  );

  const match = selectKeywordMatch(message, data.rows.map(row => ({
    id: row.id,
    name: row.name,
    enabled: row.enabled,
    priority: row.priority,
    matchMode: row.match_mode,
    responseText: row.response_text,
    keywords: row.keywords,
  })));

  res.json({ message, match });
});

api.get("/businesses/:businessId/call-logs", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  await purgeExpiredCallLogs();

  const period = String(req.query.period || "2w");
  const interval = period === "1w" ? "7 days" : period === "1m" ? "30 days" : "14 days";
  const result = await db.query(
    `SELECT id, occurred_at, call_type, masked_number, last_four, reply_status, metadata
       FROM replydesk_call_logs
      WHERE business_id = $1
        AND occurred_at >= NOW() - $2::interval
      ORDER BY occurred_at DESC
      LIMIT 500`,
    [req.params.businessId, interval]
  );
  res.json({ period, logs: result.rows });
});

api.delete("/businesses/:businessId/call-logs", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const owned = await db.query(
    "SELECT 1 FROM replydesk_businesses WHERE id = $1 AND owner_account_id = $2",
    [req.params.businessId, req.auth.accountId]
  );
  if (!owned.rowCount) return res.status(404).json({ error: "business_not_found" });

  const result = await db.query(
    "DELETE FROM replydesk_call_logs WHERE business_id = $1",
    [req.params.businessId]
  );
  res.json({ deleted: result.rowCount || 0 });
});
