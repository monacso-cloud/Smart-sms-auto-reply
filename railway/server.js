const crypto = require("crypto");
const express = require("express");
const helmet = require("helmet");
const { Pool } = require("pg");

const app = express();
const port = Number(process.env.PORT || 3000);
const adminToken = process.env.ADMIN_TOKEN || "";
const licensePepper = process.env.LICENSE_PEPPER || "";

if (!process.env.DATABASE_URL || !adminToken || !licensePepper) {
  console.error("DATABASE_URL, ADMIN_TOKEN and LICENSE_PEPPER are required");
  process.exit(1);
}

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.PGSSLMODE === "disable" ? false : { rejectUnauthorized: false }
});

app.disable("x-powered-by");
app.set("trust proxy", 1);
app.use(helmet());
app.use(express.json({ limit: "16kb" }));

const hash = (value) => crypto
  .createHash("sha256")
  .update(`${licensePepper}:${String(value).trim().toUpperCase()}`)
  .digest("hex");

const newLicenseKey = () => {
  const raw = crypto.randomBytes(18).toString("base64url").toUpperCase();
  return `SSR-${raw.slice(0, 6)}-${raw.slice(6, 12)}-${raw.slice(12, 18)}-${raw.slice(18, 24)}`;
};

async function initialize() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS licenses (
      id BIGSERIAL PRIMARY KEY,
      key_hash TEXT UNIQUE NOT NULL,
      customer_email TEXT,
      status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'revoked')),
      max_devices INTEGER NOT NULL DEFAULT 1 CHECK (max_devices BETWEEN 1 AND 20),
      expires_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS activations (
      id BIGSERIAL PRIMARY KEY,
      license_id BIGINT NOT NULL REFERENCES licenses(id) ON DELETE CASCADE,
      device_hash TEXT NOT NULL,
      app_version TEXT,
      first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (license_id, device_hash)
    );
    CREATE INDEX IF NOT EXISTS activations_license_id_idx ON activations(license_id);
  `);
}

function requireAdmin(req, res, next) {
  const supplied = req.get("authorization") || "";
  const expected = `Bearer ${adminToken}`;
  const suppliedBuffer = Buffer.from(supplied);
  const expectedBuffer = Buffer.from(expected);
  if (suppliedBuffer.length !== expectedBuffer.length
      || !crypto.timingSafeEqual(suppliedBuffer, expectedBuffer)) {
    return res.status(401).json({ ok: false, error: "unauthorized" });
  }
  next();
}

app.get("/health", (_req, res) => res.json({ ok: true, service: "smart-sms-reply-licensing" }));

app.post("/v1/admin/licenses", requireAdmin, async (req, res, next) => {
  try {
    const key = newLicenseKey();
    const maxDevices = Math.max(1, Math.min(20, Number(req.body.maxDevices || 1)));
    const expiresAt = req.body.expiresAt ? new Date(req.body.expiresAt) : null;
    if (expiresAt && Number.isNaN(expiresAt.getTime())) {
      return res.status(400).json({ ok: false, error: "invalid_expiry" });
    }
    await pool.query(
      `INSERT INTO licenses (key_hash, customer_email, max_devices, expires_at)
       VALUES ($1, $2, $3, $4)`,
      [hash(key), req.body.customerEmail || null, maxDevices, expiresAt]
    );
    res.status(201).json({ ok: true, licenseKey: key, maxDevices, expiresAt });
  } catch (error) {
    next(error);
  }
});

app.post("/v1/admin/licenses/:key/status", requireAdmin, async (req, res, next) => {
  try {
    const status = String(req.body.status || "");
    if (!['active', 'suspended', 'revoked'].includes(status)) {
      return res.status(400).json({ ok: false, error: "invalid_status" });
    }
    const result = await pool.query(
      "UPDATE licenses SET status = $1 WHERE key_hash = $2 RETURNING id",
      [status, hash(req.params.key)]
    );
    if (!result.rowCount) return res.status(404).json({ ok: false, error: "not_found" });
    res.json({ ok: true, status });
  } catch (error) {
    next(error);
  }
});

async function validateLicense(req, res, next) {
  try {
    const licenseKey = String(req.body.licenseKey || "").trim();
    const deviceHash = String(req.body.deviceHash || "").trim();
    const appVersion = String(req.body.appVersion || "").slice(0, 64);
    if (licenseKey.length < 12 || deviceHash.length < 32) {
      return res.status(400).json({ ok: false, active: false, error: "invalid_request" });
    }

    const client = await pool.connect();
    try {
      await client.query("BEGIN");
      const licenseResult = await client.query(
        `SELECT id, status, max_devices, expires_at
           FROM licenses WHERE key_hash = $1 FOR UPDATE`,
        [hash(licenseKey)]
      );
      if (!licenseResult.rowCount) {
        await client.query("ROLLBACK");
        return res.status(404).json({ ok: false, active: false, error: "license_not_found" });
      }

      const license = licenseResult.rows[0];
      if (license.status !== "active") {
        await client.query("ROLLBACK");
        return res.status(403).json({ ok: false, active: false, error: license.status });
      }
      if (license.expires_at && new Date(license.expires_at) <= new Date()) {
        await client.query("ROLLBACK");
        return res.status(403).json({ ok: false, active: false, error: "expired" });
      }

      const existing = await client.query(
        "SELECT id FROM activations WHERE license_id = $1 AND device_hash = $2",
        [license.id, deviceHash]
      );
      if (!existing.rowCount) {
        const count = await client.query(
          "SELECT COUNT(*)::int AS total FROM activations WHERE license_id = $1",
          [license.id]
        );
        if (count.rows[0].total >= license.max_devices) {
          await client.query("ROLLBACK");
          return res.status(403).json({ ok: false, active: false, error: "device_limit" });
        }
        await client.query(
          `INSERT INTO activations (license_id, device_hash, app_version)
           VALUES ($1, $2, $3)`,
          [license.id, deviceHash, appVersion]
        );
      } else {
        await client.query(
          `UPDATE activations SET last_seen_at = NOW(), app_version = $1
           WHERE license_id = $2 AND device_hash = $3`,
          [appVersion, license.id, deviceHash]
        );
      }
      await client.query("COMMIT");
      res.json({ ok: true, active: true, expiresAt: license.expires_at, offlineGraceHours: 72 });
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  } catch (error) {
    next(error);
  }
}

app.post("/v1/licenses/activate", validateLicense);
app.post("/v1/licenses/validate", validateLicense);

app.use((error, _req, res, _next) => {
  console.error(error);
  res.status(500).json({ ok: false, error: "server_error" });
});

initialize()
  .then(() => app.listen(port, "0.0.0.0", () => console.log(`Licensing API listening on ${port}`)))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
