import { Router } from "express";
import { getPool } from "./db.js";

export const internalEvents = Router();

function requireEventSecret(req, res, next) {
  const expected = process.env.STORE_EVENT_SECRET;
  const received = req.get("x-replydesk-event-secret");
  if (!expected || !received || received !== expected) {
    return res.status(401).json({ error: "unauthorized" });
  }
  next();
}

internalEvents.use(requireEventSecret);

internalEvents.post("/subscription", async (req, res) => {
  const db = getPool();
  if (!db) return res.status(503).json({ error: "database_unavailable" });

  const {
    accountId,
    platform,
    productId,
    transactionId,
    status,
    trialEndsAt,
    currentPeriodEndsAt,
    autoRenews,
  } = req.body || {};

  const validStatuses = new Set(["trial", "active", "expired", "cancelled"]);
  const validPlatforms = new Set(["apple", "google", "web", "test"]);

  if (!Number.isInteger(Number(accountId)) ||
      !validPlatforms.has(platform) ||
      !validStatuses.has(status) ||
      typeof transactionId !== "string" ||
      !transactionId) {
    return res.status(400).json({ error: "invalid_subscription_event" });
  }

  const result = await db.query(
    `INSERT INTO replydesk_subscription_entitlements
       (account_id, platform, external_product_id, external_transaction_id, status,
        trial_ends_at, current_period_ends_at, auto_renews, updated_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,NOW())
     ON CONFLICT (platform, external_transaction_id)
     DO UPDATE SET
       account_id = EXCLUDED.account_id,
       external_product_id = EXCLUDED.external_product_id,
       status = EXCLUDED.status,
       trial_ends_at = EXCLUDED.trial_ends_at,
       current_period_ends_at = EXCLUDED.current_period_ends_at,
       auto_renews = EXCLUDED.auto_renews,
       updated_at = NOW()
     RETURNING id, account_id, platform, status, trial_ends_at,
               current_period_ends_at, auto_renews, updated_at`,
    [
      Number(accountId),
      platform,
      productId || null,
      transactionId,
      status,
      trialEndsAt || null,
      currentPeriodEndsAt || null,
      Boolean(autoRenews),
    ]
  );

  res.json({ entitlement: result.rows[0] });
});
