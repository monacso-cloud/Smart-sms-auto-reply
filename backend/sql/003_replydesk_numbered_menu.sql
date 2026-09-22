CREATE TABLE IF NOT EXISTS replydesk_numbered_menu_settings (
  business_id BIGINT PRIMARY KEY REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  intro_text TEXT NOT NULL DEFAULT 'Automated assistant: How can we help? Reply with a number:',
  automation_disclosure TEXT NOT NULL DEFAULT 'Automated reply:',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS replydesk_numbered_menu_items (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT NOT NULL REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  option_number INTEGER NOT NULL,
  label TEXT NOT NULL DEFAULT '',
  reply_text TEXT NOT NULL DEFAULT '',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(business_id, option_number),
  CHECK (option_number BETWEEN 1 AND 10)
);

CREATE INDEX IF NOT EXISTS replydesk_numbered_menu_business_idx
  ON replydesk_numbered_menu_items(business_id, option_number);
