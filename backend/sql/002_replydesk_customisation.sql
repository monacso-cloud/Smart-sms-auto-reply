CREATE TABLE IF NOT EXISTS replydesk_keyword_groups (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT NOT NULL REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  priority INTEGER NOT NULL DEFAULT 100,
  match_mode TEXT NOT NULL DEFAULT 'contains',
  response_text TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS replydesk_keywords (
  id BIGSERIAL PRIMARY KEY,
  group_id BIGINT NOT NULL REFERENCES replydesk_keyword_groups(id) ON DELETE CASCADE,
  keyword TEXT NOT NULL,
  normalized_keyword TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(group_id, normalized_keyword)
);

CREATE TABLE IF NOT EXISTS replydesk_reply_settings (
  business_id BIGINT PRIMARY KEY REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  master_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  reply_to_missed_calls BOOLEAN NOT NULL DEFAULT TRUE,
  reply_to_incoming_sms BOOLEAN NOT NULL DEFAULT FALSE,
  schedule_mode TEXT NOT NULL DEFAULT 'always',
  timezone TEXT NOT NULL DEFAULT 'Australia/Perth',
  call_log_retention_days INTEGER NOT NULL DEFAULT 14,
  auto_delete_logs BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (call_log_retention_days IN (7,14,30))
);

CREATE TABLE IF NOT EXISTS replydesk_schedule_windows (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT NOT NULL REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  day_of_week INTEGER NOT NULL,
  start_local_time TIME NOT NULL,
  end_local_time TIME NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (day_of_week BETWEEN 0 AND 6)
);

CREATE TABLE IF NOT EXISTS replydesk_call_logs (
  id BIGSERIAL PRIMARY KEY,
  business_id BIGINT NOT NULL REFERENCES replydesk_businesses(id) ON DELETE CASCADE,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  call_type TEXT NOT NULL,
  masked_number TEXT,
  last_four TEXT,
  reply_status TEXT,
  reply_event_id BIGINT REFERENCES replydesk_message_events(id) ON DELETE SET NULL,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS replydesk_keyword_group_business_idx
  ON replydesk_keyword_groups(business_id, enabled, priority);
CREATE INDEX IF NOT EXISTS replydesk_keyword_group_idx
  ON replydesk_keywords(group_id);
CREATE INDEX IF NOT EXISTS replydesk_schedule_business_idx
  ON replydesk_schedule_windows(business_id, day_of_week, enabled);
CREATE INDEX IF NOT EXISTS replydesk_call_log_business_time_idx
  ON replydesk_call_logs(business_id, occurred_at DESC);
