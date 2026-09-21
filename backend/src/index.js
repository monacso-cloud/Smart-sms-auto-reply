import express from "express";
import { checkDatabase } from "./db.js";
import { api } from "./api.js";

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "256kb" }));

app.get("/health", async (_req, res) => {
  const database = await checkDatabase();
  const ok = !database.configured || database.reachable;
  res.status(ok ? 200 : 503).json({
    service: "replydesk-backend",
    status: ok ? "ok" : "degraded",
    database,
  });
});

app.use("/api/v1", api);

app.get("/api/v1/platform-info", (_req, res) => {
  res.json({
    product: "ReplyDesk",
    capabilities: {
      missedCallReply: true,
      smsAutomation: true,
      scheduledRules: true,
      teamHandover: true,
      subscriptionEntitlements: true,
    },
  });
});

const port = Number.parseInt(process.env.PORT || "3000", 10);
app.listen(port, "0.0.0.0", () => {
  console.log(`ReplyDesk backend listening on port ${port}`);
});
