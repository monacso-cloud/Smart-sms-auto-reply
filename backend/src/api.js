import { Router } from "express";
import { getPool } from "./db.js";
import { requireAuth } from "./auth.js";

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
