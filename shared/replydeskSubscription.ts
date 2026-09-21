export const REPLYDESK_SUBSCRIPTION = {
  trialDays: 14,
  monthly: {
    priceAud: 30,
    productId: "replydesk_monthly",
  },
  yearly: {
    priceAud: 299,
    productId: "replydesk_yearly",
  },
  statuses: ["trial", "active", "expired", "cancelled"],
} as const;
