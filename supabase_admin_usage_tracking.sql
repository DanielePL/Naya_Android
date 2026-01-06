-- ═══════════════════════════════════════════════════════════════
-- PROMETHEUS ADMIN - USAGE & COST TRACKING SCHEMA
-- Feature Branch: feature/admin-cost-tracking
-- ═══════════════════════════════════════════════════════════════

-- 1. ADMIN USERS TABLE (define who can access admin data)
CREATE TABLE IF NOT EXISTS admin_users (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT NOT NULL,
  role TEXT DEFAULT 'admin' CHECK (role IN ('admin', 'super_admin')),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. USAGE LOGS TABLE (track all cost-generating events)
CREATE TABLE IF NOT EXISTS usage_logs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,

  -- Event Type
  event_type TEXT NOT NULL CHECK (event_type IN (
    'openai_vision',      -- Meal photo analysis (GPT-4o Vision)
    'openai_chat',        -- Any OpenAI chat completion
    'claude_chat',        -- Claude API calls (if enabled)
    'vbt_analysis',       -- Form/VBT video analysis on Render
    'ai_coach_chat',      -- AI Coach backend chat
    'storage_upload',     -- Supabase storage upload
    'storage_download'    -- Supabase storage download
  )),

  -- Token Usage (for LLM calls)
  input_tokens INTEGER DEFAULT 0,
  output_tokens INTEGER DEFAULT 0,
  total_tokens INTEGER DEFAULT 0,

  -- Cost Estimation
  estimated_cost DECIMAL(10, 6) DEFAULT 0,

  -- Additional Metadata
  metadata JSONB DEFAULT '{}',
  -- Example metadata:
  -- For openai_vision: {"model": "gpt-4o", "image_size_kb": 512}
  -- For vbt_analysis: {"video_duration_sec": 30, "exercise_type": "squat"}
  -- For storage_upload: {"bucket": "meal-photos", "file_size_bytes": 524288}

  -- Request Info
  request_duration_ms INTEGER,
  success BOOLEAN DEFAULT true,
  error_message TEXT,

  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. DAILY COST SUMMARY VIEW (aggregated costs per user per day)
CREATE OR REPLACE VIEW daily_cost_summary AS
SELECT
  DATE(created_at) as date,
  user_id,
  COUNT(*) FILTER (WHERE event_type = 'openai_vision') as openai_vision_calls,
  COUNT(*) FILTER (WHERE event_type = 'vbt_analysis') as vbt_analysis_calls,
  COUNT(*) FILTER (WHERE event_type = 'ai_coach_chat') as ai_coach_messages,
  COUNT(*) FILTER (WHERE event_type = 'storage_upload') as storage_uploads,
  SUM(total_tokens) as total_tokens,
  SUM(estimated_cost) as total_estimated_cost,
  COUNT(*) as total_events
FROM usage_logs
GROUP BY DATE(created_at), user_id
ORDER BY date DESC, total_estimated_cost DESC;

-- 4. MONTHLY COST SUMMARY VIEW (for billing overview)
CREATE OR REPLACE VIEW monthly_cost_summary AS
SELECT
  DATE_TRUNC('month', created_at) as month,
  COUNT(DISTINCT user_id) as unique_users,
  COUNT(*) FILTER (WHERE event_type = 'openai_vision') as openai_vision_calls,
  COUNT(*) FILTER (WHERE event_type = 'vbt_analysis') as vbt_analysis_calls,
  COUNT(*) FILTER (WHERE event_type = 'ai_coach_chat') as ai_coach_messages,
  SUM(total_tokens) as total_tokens,
  SUM(estimated_cost) as total_estimated_cost,
  SUM(estimated_cost) / NULLIF(COUNT(DISTINCT user_id), 0) as avg_cost_per_user,
  COUNT(*) as total_events
FROM usage_logs
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month DESC;

-- 5. USER COST SUMMARY VIEW (lifetime costs per user)
CREATE OR REPLACE VIEW user_cost_summary AS
SELECT
  user_id,
  MIN(created_at) as first_activity,
  MAX(created_at) as last_activity,
  COUNT(DISTINCT DATE(created_at)) as active_days,
  COUNT(*) FILTER (WHERE event_type = 'openai_vision') as openai_vision_calls,
  COUNT(*) FILTER (WHERE event_type = 'vbt_analysis') as vbt_analysis_calls,
  COUNT(*) FILTER (WHERE event_type = 'ai_coach_chat') as ai_coach_messages,
  SUM(total_tokens) as total_tokens,
  SUM(estimated_cost) as total_estimated_cost,
  COUNT(*) as total_events
FROM usage_logs
GROUP BY user_id
ORDER BY total_estimated_cost DESC;

-- 6. COST RATES TABLE (configurable pricing)
CREATE TABLE IF NOT EXISTS cost_rates (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_type TEXT NOT NULL UNIQUE,

  -- Token-based pricing (for LLM calls)
  input_token_cost DECIMAL(12, 8) DEFAULT 0,   -- Cost per input token
  output_token_cost DECIMAL(12, 8) DEFAULT 0,  -- Cost per output token

  -- Fixed pricing (for other services)
  base_cost DECIMAL(10, 6) DEFAULT 0,          -- Base cost per call

  -- Per-unit pricing (for storage, etc.)
  unit_cost DECIMAL(12, 8) DEFAULT 0,          -- Cost per unit
  unit_type TEXT,                               -- 'byte', 'second', etc.

  description TEXT,
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insert default cost rates (current pricing as of 2024)
INSERT INTO cost_rates (event_type, input_token_cost, output_token_cost, description) VALUES
  ('openai_vision', 0.0000025, 0.00001, 'GPT-4o Vision: $2.50/1M input, $10/1M output'),
  ('openai_chat', 0.0000025, 0.00001, 'GPT-4o Chat: $2.50/1M input, $10/1M output'),
  ('claude_chat', 0.00000025, 0.00000125, 'Claude 3 Haiku: $0.25/1M input, $1.25/1M output')
ON CONFLICT (event_type) DO NOTHING;

INSERT INTO cost_rates (event_type, base_cost, description) VALUES
  ('vbt_analysis', 0.001, 'Estimated Render compute cost per video analysis'),
  ('ai_coach_chat', 0.002, 'Backend AI processing cost per message')
ON CONFLICT (event_type) DO NOTHING;

INSERT INTO cost_rates (event_type, unit_cost, unit_type, description) VALUES
  ('storage_upload', 0.0000005, 'byte', 'Supabase Storage: ~$0.50/GB'),
  ('storage_download', 0.0000001, 'byte', 'Supabase Egress: ~$0.10/GB')
ON CONFLICT (event_type) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- INDEXES (for performance)
-- ═══════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_usage_logs_user_id ON usage_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_usage_logs_event_type ON usage_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_usage_logs_created_at ON usage_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_logs_user_date ON usage_logs(user_id, DATE(created_at) DESC);

-- Composite index for common admin queries
CREATE INDEX IF NOT EXISTS idx_usage_logs_admin_query
  ON usage_logs(created_at DESC, event_type, user_id);

-- ═══════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY (RLS)
-- ═══════════════════════════════════════════════════════════════

-- Enable RLS
ALTER TABLE usage_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE cost_rates ENABLE ROW LEVEL SECURITY;

-- USAGE LOGS: Only admins can read, service role can insert
DROP POLICY IF EXISTS "Service role can insert usage logs" ON usage_logs;
CREATE POLICY "Service role can insert usage logs" ON usage_logs
  FOR INSERT WITH CHECK (true);  -- App inserts via service role

DROP POLICY IF EXISTS "Admins can view all usage logs" ON usage_logs;
CREATE POLICY "Admins can view all usage logs" ON usage_logs
  FOR SELECT USING (
    auth.uid() IN (SELECT id FROM admin_users)
  );

-- ADMIN USERS: Only super_admins can manage
DROP POLICY IF EXISTS "Admins can view admin list" ON admin_users;
CREATE POLICY "Admins can view admin list" ON admin_users
  FOR SELECT USING (
    auth.uid() IN (SELECT id FROM admin_users)
  );

DROP POLICY IF EXISTS "Super admins can manage admins" ON admin_users;
CREATE POLICY "Super admins can manage admins" ON admin_users
  FOR ALL USING (
    auth.uid() IN (SELECT id FROM admin_users WHERE role = 'super_admin')
  );

-- COST RATES: Only admins can view, super_admins can modify
DROP POLICY IF EXISTS "Admins can view cost rates" ON cost_rates;
CREATE POLICY "Admins can view cost rates" ON cost_rates
  FOR SELECT USING (
    auth.uid() IN (SELECT id FROM admin_users)
  );

DROP POLICY IF EXISTS "Super admins can modify cost rates" ON cost_rates;
CREATE POLICY "Super admins can modify cost rates" ON cost_rates
  FOR ALL USING (
    auth.uid() IN (SELECT id FROM admin_users WHERE role = 'super_admin')
  );

-- ═══════════════════════════════════════════════════════════════
-- HELPER FUNCTIONS
-- ═══════════════════════════════════════════════════════════════

-- Function: Calculate estimated cost for a usage event
CREATE OR REPLACE FUNCTION calculate_usage_cost(
  p_event_type TEXT,
  p_input_tokens INTEGER DEFAULT 0,
  p_output_tokens INTEGER DEFAULT 0,
  p_units BIGINT DEFAULT 0
)
RETURNS DECIMAL(10, 6)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
  v_rate cost_rates%ROWTYPE;
  v_cost DECIMAL(10, 6) := 0;
BEGIN
  SELECT * INTO v_rate FROM cost_rates WHERE event_type = p_event_type;

  IF NOT FOUND THEN
    RETURN 0;
  END IF;

  -- Token-based cost
  v_cost := v_cost + (p_input_tokens * v_rate.input_token_cost);
  v_cost := v_cost + (p_output_tokens * v_rate.output_token_cost);

  -- Base cost
  v_cost := v_cost + v_rate.base_cost;

  -- Unit-based cost
  IF v_rate.unit_cost > 0 AND p_units > 0 THEN
    v_cost := v_cost + (p_units * v_rate.unit_cost);
  END IF;

  RETURN v_cost;
END;
$$;

-- Function: Log usage event with auto-calculated cost
CREATE OR REPLACE FUNCTION log_usage_event(
  p_user_id UUID,
  p_event_type TEXT,
  p_input_tokens INTEGER DEFAULT 0,
  p_output_tokens INTEGER DEFAULT 0,
  p_metadata JSONB DEFAULT '{}',
  p_duration_ms INTEGER DEFAULT NULL,
  p_success BOOLEAN DEFAULT true,
  p_error_message TEXT DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_log_id UUID;
  v_estimated_cost DECIMAL(10, 6);
  v_units BIGINT := 0;
BEGIN
  -- Extract units from metadata if present (for storage events)
  IF p_metadata ? 'file_size_bytes' THEN
    v_units := (p_metadata->>'file_size_bytes')::BIGINT;
  END IF;

  -- Calculate cost
  v_estimated_cost := calculate_usage_cost(p_event_type, p_input_tokens, p_output_tokens, v_units);

  -- Insert log
  INSERT INTO usage_logs (
    user_id, event_type, input_tokens, output_tokens, total_tokens,
    estimated_cost, metadata, request_duration_ms, success, error_message
  ) VALUES (
    p_user_id, p_event_type, p_input_tokens, p_output_tokens, p_input_tokens + p_output_tokens,
    v_estimated_cost, p_metadata, p_duration_ms, p_success, p_error_message
  )
  RETURNING id INTO v_log_id;

  RETURN v_log_id;
END;
$$;

-- ═══════════════════════════════════════════════════════════════
-- ADMIN SETUP: Add yourself as admin (run this manually!)
-- ═══════════════════════════════════════════════════════════════

-- IMPORTANT: Replace with your actual Supabase user ID and email
-- Run this in SQL Editor after creating your account:
--
-- INSERT INTO admin_users (id, email, role) VALUES
--   ('your-user-uuid-here', 'your-email@example.com', 'super_admin');

-- ═══════════════════════════════════════════════════════════════
-- DONE! Run this SQL in Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════