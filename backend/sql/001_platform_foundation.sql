CREATE TABLE IF NOT EXISTS replydesk_accounts (
  id BIGSERIAL PRIMARY KEY,
  email TEXT UNIQUE,
  display_name TEXT,
  status TEXT NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS replydesk_businesses (
  id BIGSERIAL PRIMARY KEY,
  owner_account_id BIGINT NOT NULL REFERENCES replydesk_accounts(id),
  name TEXT NOT NULL,
  timezone TEXT NOT NULL DEFAULT 'Australia/Perth',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS replydesk_staff_members (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT NOT NULL REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  account_id BIGINT REFERENCES replydesk_accounts(id),
  role TEXT NOT NULL DEFAULT 'staff',
  department TEXT,
  can_take_handover BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS replydesk_subscription_entitlements (
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT NOT NULL REFERENCES replydesk_accounts(id) ON DELETE CASCADE,
  platform TEXT NOT NULL,
  external_product_id TEXT,
  external_transaction_id TEXT,
  status TEXT NOT NULL,
  trial_ends_at TIMESTAMPTZ,
  current_period_ends_at TIMESTAMPTZ,
  auto_renews BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(platform, external_transaction_id)
);

CREATE TABLE IF NOT EXISTS replydesk_automation_rules (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT NOT NULL REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  trigger_type TEXT NOT NULL,
  start_local_time TIME,
  end_local_time TIME,
  reply_mode TEXT NOT NULL DEFAULT 'missed_call',
  message_template TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS replydesk_message_events (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  direction TEXT NOT NULL,
  channel TEXT NOT NULL,
  external_reference TEXT,
  masked_contact TEXT,
  status TEXT NOT NULL,
  event_type TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS replydesk_handovers (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT NOT NULL REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  message_event_id BIGINT REFERENCES replydesk_message_events(id),
  department TEXT,
  assigned_staff_id BIGINT REFERENCES replydesk_staff_members(id),
  status TEXT NOT NULL DEFAULT 'open',
  accepted_at TIMESTAMPTZ,
  closed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS replydesk_entitlement_account_idx
  ON replydesk_subscription_entitlements(account_id, status);
CREATE INDEX IF NOT EXISTS replydesk_rules_business_idx
  ON replydesk_automation_rules(business_id, enabled);
CREATE INDEX IF NOT EXISTS replydesk_handover_business_status_idx
  ON replydesk_handovers(business_id, status);
