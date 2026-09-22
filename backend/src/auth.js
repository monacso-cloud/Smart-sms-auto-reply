import { jwtVerify } from "jose";

function getSecret() {
  const value = process.env.JWT_SECRET;
  if (!value) throw new Error("JWT_SECRET is not configured");
  return new TextEncoder().encode(value);
}

export async function requireAuth(req, res, next) {
  const header = req.get("authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!token) return res.status(401).json({ error: "unauthorized" });

  try {
    const { payload } = await jwtVerify(token, getSecret(), {
      algorithms: ["HS256"],
      issuer: "replydesk",
      audience: "replydesk-app",
    });
    if (!payload.sub) return res.status(401).json({ error: "unauthorized" });
    req.auth = { accountId: Number(payload.sub), role: String(payload.role || "owner") };
    next();
  } catch {
    return res.status(401).json({ error: "unauthorized" });
  }
}
