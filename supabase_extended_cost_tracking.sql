-- ═══════════════════════════════════════════════════════════════
-- PROMETHEUS ADMIN - EXTENDED COST TRACKING SCHEMA
-- Adds: Fixed Costs, Labor, Subscriptions, App Store Revenue
-- ═══════════════════════════════════════════════════════════════

-- 1. FIXED COSTS TABLE (manual entries for labor, subscriptions, etc.)
CREATE TABLE IF NOT EXISTS fixed_costs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Cost Category
  category TEXT NOT NULL CHECK (category IN (
    'labor',              -- Developer salaries, contractor costs
    'subscription',       -- Claude Max, ChatGPT Plus, etc.
    'infrastructure',     -- Server costs not tracked automatically
    'marketing',          -- Ads, ASO, etc.
    'app_store_fees',     -- Apple Developer ($99/yr), Google Play ($25)
    'other'               -- Miscellaneous
  )),

  -- Cost Details
  name TEXT NOT NULL,
  description TEXT,
  amount DECIMAL(12, 2) NOT NULL,
  currency TEXT DEFAULT 'USD',

  -- Recurrence
  is_recurring BOOLEAN DEFAULT true,
  recurrence_type TEXT CHECK (recurrence_type IN ('monthly', 'yearly', 'one_time')),

  -- Dates
  start_date DATE,
  end_date DATE,  -- NULL = ongoing

  -- Metadata
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by UUID REFERENCES auth.users(id)
);

-- 2. APP REVENUE TABLE (for calculating app store commissions)
CREATE TABLE IF NOT EXISTS app_revenue (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Revenue Details
  platform TEXT NOT NULL CHECK (platform IN ('ios', 'android', 'web')),
  revenue_type TEXT NOT NULL CHECK (revenue_type IN (
    'subscription',       -- Recurring subscription
    'one_time_purchase',  -- IAP
    'ad_revenue',         -- Ads (not subject to app store cut)
    'other'
  )),

  -- Amounts
  gross_revenue DECIMAL(12, 2) NOT NULL,
  app_store_commission DECIMAL(12, 2) DEFAULT 0,  -- Calculated: 30% or 15%
  net_revenue DECIMAL(12, 2),  -- After commission

  -- Date Range
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,

  -- Metadata
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. BUSINESS METRICS TABLE (for configurable rates)
CREATE TABLE IF NOT EXISTS business_metrics (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  metric_key TEXT UNIQUE NOT NULL,
  metric_value DECIMAL(12, 4) NOT NULL,
  description TEXT,
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insert default business metrics
INSERT INTO business_metrics (metric_key, metric_value, description) VALUES
  -- App Store Commissions
  ('apple_commission_rate', 0.30, 'Apple App Store commission (30%, or 15% for small business)'),
  ('google_commission_rate', 0.30, 'Google Play Store commission (30%, or 15% for small business)'),
  -- Industry Cost Benchmarks
  ('avg_fitness_app_infra_cost', 0.50, 'Industry avg infrastructure cost per active user/month'),
  ('avg_fitness_app_support_cost', 0.20, 'Industry avg support cost per active user/month'),
  -- Reference Costs
  ('claude_max_monthly', 100.00, 'Claude Max Plan monthly cost'),
  ('chatgpt_plus_monthly', 20.00, 'ChatGPT Plus monthly cost'),
  ('render_monthly_estimate', 50.00, 'Render backend monthly estimate'),
  ('supabase_monthly_estimate', 25.00, 'Supabase monthly estimate'),
  -- ═══════════════════════════════════════════════════════════════
  -- SUBSCRIPTION PRICING (ADJUST TO YOUR ACTUAL PRICES!)
  -- ═══════════════════════════════════════════════════════════════
  ('monthly_sub_price', 9.99, 'Monthly subscription price (gross, before app store cut)'),
  ('yearly_sub_price', 59.99, 'Yearly subscription price (gross, before app store cut)'),
  -- ═══════════════════════════════════════════════════════════════
  -- INDUSTRY BENCHMARKS - FITNESS APPS (Source: RevenueCat, Adjust, Statista)
  -- ═══════════════════════════════════════════════════════════════
  ('monthly_churn_rate', 0.12, 'Monthly subscriber churn rate (~12%/month = ~4.5 months avg lifetime)'),
  ('yearly_renewal_rate', 0.45, 'Yearly subscriber renewal rate (~45% renew, fitness apps lower than avg)'),
  ('monthly_to_yearly_ratio', 0.65, 'Typical split: 65% monthly, 35% yearly subscribers'),
  ('avg_monthly_ltv_months', 4.5, 'Average lifetime of monthly subscriber in months (1/0.12 ≈ 8, but realistic ~4.5)'),
  ('avg_yearly_renewals', 1.8, 'Average total years a yearly subscriber stays (initial + 0.8 renewals)'),
  ('variable_cost_per_active_user', 0.05, 'Estimated variable API cost per active user/month')
ON CONFLICT (metric_key) DO NOTHING;

-- 4. COMPREHENSIVE COST SUMMARY VIEW
CREATE OR REPLACE VIEW comprehensive_cost_summary AS
WITH
-- Variable costs from usage_logs (current month)
variable_costs AS (
  SELECT
    COALESCE(SUM(estimated_cost), 0) as total_variable_cost,
    COUNT(DISTINCT user_id) as active_users,
    COUNT(*) as total_events
  FROM usage_logs
  WHERE created_at >= DATE_TRUNC('month', CURRENT_DATE)
),
-- Fixed costs (monthly equivalent)
monthly_fixed AS (
  SELECT COALESCE(SUM(
    CASE
      WHEN recurrence_type = 'monthly' THEN amount
      WHEN recurrence_type = 'yearly' THEN amount / 12
      ELSE 0  -- one_time costs handled separately
    END
  ), 0) as monthly_fixed_cost
  FROM fixed_costs
  WHERE is_recurring = true
    AND (end_date IS NULL OR end_date >= CURRENT_DATE)
    AND (start_date IS NULL OR start_date <= CURRENT_DATE)
),
-- One-time costs this month
one_time_costs AS (
  SELECT COALESCE(SUM(amount), 0) as one_time_cost
  FROM fixed_costs
  WHERE recurrence_type = 'one_time'
    AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', CURRENT_DATE)
),
-- Revenue and commissions this month
revenue AS (
  SELECT
    COALESCE(SUM(gross_revenue), 0) as total_gross_revenue,
    COALESCE(SUM(app_store_commission), 0) as total_commission,
    COALESCE(SUM(net_revenue), 0) as total_net_revenue
  FROM app_revenue
  WHERE period_start >= DATE_TRUNC('month', CURRENT_DATE)
)
SELECT
  -- Variable Costs
  vc.total_variable_cost,
  vc.active_users,
  vc.total_events,

  -- Fixed Costs
  mf.monthly_fixed_cost,
  otc.one_time_cost,

  -- Total Costs
  (vc.total_variable_cost + mf.monthly_fixed_cost + otc.one_time_cost) as total_monthly_cost,

  -- Revenue
  r.total_gross_revenue,
  r.total_commission as app_store_commission,
  r.total_net_revenue,

  -- Profit/Loss
  (r.total_net_revenue - (vc.total_variable_cost + mf.monthly_fixed_cost + otc.one_time_cost)) as monthly_profit,

  -- Per-User Metrics
  CASE WHEN vc.active_users > 0
    THEN (vc.total_variable_cost + mf.monthly_fixed_cost + otc.one_time_cost) / vc.active_users
    ELSE 0
  END as cost_per_active_user,

  CASE WHEN vc.active_users > 0
    THEN r.total_net_revenue / vc.active_users
    ELSE 0
  END as revenue_per_active_user

FROM variable_costs vc
CROSS JOIN monthly_fixed mf
CROSS JOIN one_time_costs otc
CROSS JOIN revenue r;

-- 5. FIXED COSTS BY CATEGORY VIEW
CREATE OR REPLACE VIEW fixed_costs_by_category AS
SELECT
  category,
  COUNT(*) as item_count,
  SUM(CASE
    WHEN recurrence_type = 'monthly' THEN amount
    WHEN recurrence_type = 'yearly' THEN amount / 12
    ELSE 0
  END) as monthly_equivalent,
  SUM(CASE
    WHEN recurrence_type = 'yearly' THEN amount
    ELSE 0
  END) as yearly_total,
  string_agg(name, ', ') as items
FROM fixed_costs
WHERE is_recurring = true
  AND (end_date IS NULL OR end_date >= CURRENT_DATE)
GROUP BY category
ORDER BY monthly_equivalent DESC;

-- 6. COST COMPARISON VIEW (your costs vs industry average)
CREATE OR REPLACE VIEW cost_comparison AS
WITH your_costs AS (
  SELECT * FROM comprehensive_cost_summary
),
industry_avg AS (
  SELECT
    (SELECT metric_value FROM business_metrics WHERE metric_key = 'avg_fitness_app_infra_cost') as infra_cost,
    (SELECT metric_value FROM business_metrics WHERE metric_key = 'avg_fitness_app_support_cost') as support_cost
)
SELECT
  yc.active_users,
  yc.cost_per_active_user as your_cost_per_user,
  (ia.infra_cost + ia.support_cost) as industry_avg_cost_per_user,
  yc.cost_per_active_user - (ia.infra_cost + ia.support_cost) as cost_difference,
  CASE
    WHEN yc.cost_per_active_user < (ia.infra_cost + ia.support_cost) THEN 'Below Average'
    WHEN yc.cost_per_active_user > (ia.infra_cost + ia.support_cost) * 1.5 THEN 'High'
    ELSE 'Average'
  END as cost_status
FROM your_costs yc
CROSS JOIN industry_avg ia;

-- ═══════════════════════════════════════════════════════════════
-- INDEXES
-- ═══════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_fixed_costs_category ON fixed_costs(category);
CREATE INDEX IF NOT EXISTS idx_fixed_costs_recurring ON fixed_costs(is_recurring, end_date);
CREATE INDEX IF NOT EXISTS idx_app_revenue_period ON app_revenue(period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_app_revenue_platform ON app_revenue(platform);

-- ═══════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE fixed_costs ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_revenue ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_metrics ENABLE ROW LEVEL SECURITY;

-- Only admins can view/modify these tables
DROP POLICY IF EXISTS "Admins can manage fixed costs" ON fixed_costs;
CREATE POLICY "Admins can manage fixed costs" ON fixed_costs
  FOR ALL USING (
    auth.uid() IN (SELECT id FROM admin_users)
  );

DROP POLICY IF EXISTS "Admins can manage app revenue" ON app_revenue;
CREATE POLICY "Admins can manage app revenue" ON app_revenue
  FOR ALL USING (
    auth.uid() IN (SELECT id FROM admin_users)
  );

DROP POLICY IF EXISTS "Admins can view business metrics" ON business_metrics;
CREATE POLICY "Admins can view business metrics" ON business_metrics
  FOR SELECT USING (
    auth.uid() IN (SELECT id FROM admin_users)
  );

DROP POLICY IF EXISTS "Super admins can modify business metrics" ON business_metrics;
CREATE POLICY "Super admins can modify business metrics" ON business_metrics
  FOR ALL USING (
    auth.uid() IN (SELECT id FROM admin_users WHERE role = 'super_admin')
  );

-- ═══════════════════════════════════════════════════════════════
-- HELPER FUNCTIONS
-- ═══════════════════════════════════════════════════════════════

-- Function: Calculate app store commission
CREATE OR REPLACE FUNCTION calculate_app_store_commission()
RETURNS TRIGGER AS $$
DECLARE
  commission_rate DECIMAL(4, 2);
BEGIN
  -- Get commission rate based on platform
  IF NEW.platform = 'ios' THEN
    SELECT metric_value INTO commission_rate
    FROM business_metrics WHERE metric_key = 'apple_commission_rate';
  ELSIF NEW.platform = 'android' THEN
    SELECT metric_value INTO commission_rate
    FROM business_metrics WHERE metric_key = 'google_commission_rate';
  ELSE
    commission_rate := 0;  -- Web has no app store commission
  END IF;

  -- Only charge commission on subscriptions and IAP
  IF NEW.revenue_type IN ('subscription', 'one_time_purchase') THEN
    NEW.app_store_commission := NEW.gross_revenue * COALESCE(commission_rate, 0.30);
  ELSE
    NEW.app_store_commission := 0;
  END IF;

  NEW.net_revenue := NEW.gross_revenue - NEW.app_store_commission;
  NEW.updated_at := NOW();

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for auto-calculating commission
DROP TRIGGER IF EXISTS calculate_commission_trigger ON app_revenue;
CREATE TRIGGER calculate_commission_trigger
  BEFORE INSERT OR UPDATE ON app_revenue
  FOR EACH ROW
  EXECUTE FUNCTION calculate_app_store_commission();

-- ═══════════════════════════════════════════════════════════════
-- SAMPLE DATA (Optional - remove in production)
-- ═══════════════════════════════════════════════════════════════

-- Example: Add your actual fixed costs
-- INSERT INTO fixed_costs (category, name, description, amount, recurrence_type) VALUES
--   ('subscription', 'Claude Max Plan', 'Anthropic Claude Max subscription', 100.00, 'monthly'),
--   ('subscription', 'ChatGPT Plus', 'OpenAI ChatGPT Plus subscription', 20.00, 'monthly'),
--   ('infrastructure', 'Render Backend', 'Backend server hosting', 50.00, 'monthly'),
--   ('infrastructure', 'Supabase Pro', 'Database and storage', 25.00, 'monthly'),
--   ('app_store_fees', 'Apple Developer Program', 'Annual iOS developer fee', 99.00, 'yearly'),
--   ('app_store_fees', 'Google Play Console', 'One-time Android developer fee', 25.00, 'one_time'),
--   ('labor', 'Developer Salary', 'Main developer monthly compensation', 5000.00, 'monthly');

-- ═══════════════════════════════════════════════════════════════
-- DONE! Run this SQL in Supabase SQL Editor after the base migration
-- ═══════════════════════════════════════════════════════════════