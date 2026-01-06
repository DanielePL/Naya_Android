-- ============================================================
-- PROMETHEUS PARTNER REFERRAL SYSTEM
-- Complete Database Schema for Partner/Influencer Management
-- ============================================================

-- IMPORTANT: Run these statements in order. The partner_payouts table
-- must be created before partner_referrals due to foreign key dependency.

-- ============================================================
-- 1. PARTNERS TABLE - Core partner information
-- ============================================================
CREATE TABLE IF NOT EXISTS partners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Basic Info
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    phone TEXT,

    -- Referral Code (unique identifier for tracking)
    referral_code TEXT NOT NULL UNIQUE,

    -- Commission Settings
    commission_percent DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    commission_type TEXT NOT NULL DEFAULT 'percentage' CHECK (commission_type IN ('percentage', 'fixed')),

    -- Partner Classification
    partner_type TEXT NOT NULL DEFAULT 'affiliate' CHECK (partner_type IN (
        'influencer',      -- Social media influencers
        'gym',             -- Gym partnerships
        'coach',           -- Personal trainers/coaches
        'affiliate',       -- General affiliates
        'ambassador',      -- Brand ambassadors
        'media'            -- Media/Press partners
    )),

    -- Status
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN (
        'pending',         -- Application submitted, awaiting approval
        'active',          -- Actively earning commissions
        'paused',          -- Temporarily paused
        'terminated'       -- Partnership ended
    )),

    -- Social Media & Reach
    instagram_handle TEXT,
    tiktok_handle TEXT,
    youtube_channel TEXT,
    website_url TEXT,
    follower_count INTEGER,

    -- Payout Information
    payout_method TEXT CHECK (payout_method IN ('paypal', 'bank_transfer', 'wise', 'crypto', 'other')),
    payout_details JSONB DEFAULT '{}',  -- Flexible storage for payment info
    payout_threshold DECIMAL(10,2) DEFAULT 50.00,  -- Minimum payout amount
    payout_currency TEXT DEFAULT 'USD',

    -- Contract & Terms
    contract_signed_at TIMESTAMPTZ,
    contract_expires_at TIMESTAMPTZ,
    custom_terms JSONB DEFAULT '{}',

    -- Internal Notes
    notes TEXT,
    internal_rating INTEGER CHECK (internal_rating BETWEEN 1 AND 5),

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES auth.users(id)
);

-- Index for fast referral code lookups
CREATE INDEX IF NOT EXISTS idx_partners_referral_code ON partners(referral_code);
CREATE INDEX IF NOT EXISTS idx_partners_status ON partners(status);
CREATE INDEX IF NOT EXISTS idx_partners_email ON partners(email);


-- ============================================================
-- 2. PARTNER PAYOUTS TABLE - Tracks commission payments
-- (Created BEFORE partner_referrals because of FK reference)
-- ============================================================
CREATE TABLE IF NOT EXISTS partner_payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relationship
    partner_id UUID NOT NULL REFERENCES partners(id) ON DELETE CASCADE,

    -- Payout Details
    amount DECIMAL(10,2) NOT NULL,
    currency TEXT NOT NULL DEFAULT 'USD',
    referral_count INTEGER NOT NULL,  -- How many referrals in this payout

    -- Period covered
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,

    -- Status
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN (
        'pending',         -- Calculated, awaiting approval
        'approved',        -- Approved, ready to send
        'processing',      -- Payment in progress
        'completed',       -- Successfully paid
        'failed',          -- Payment failed
        'cancelled'        -- Cancelled
    )),

    -- Payment Details
    payout_method TEXT,
    payout_reference TEXT,  -- Transaction ID, PayPal reference, etc.
    payout_notes TEXT,

    -- Timestamps
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES auth.users(id),
    processed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payouts_partner ON partner_payouts(partner_id);
CREATE INDEX IF NOT EXISTS idx_payouts_status ON partner_payouts(status);
CREATE INDEX IF NOT EXISTS idx_payouts_period ON partner_payouts(period_start, period_end);


-- ============================================================
-- 3. PARTNER REFERRALS TABLE - Tracks each referred user
-- ============================================================
CREATE TABLE IF NOT EXISTS partner_referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relationships
    partner_id UUID NOT NULL REFERENCES partners(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Referral Details
    referral_code_used TEXT NOT NULL,
    referral_source TEXT,  -- Where they came from (instagram, tiktok, website, etc.)

    -- Subscription Info (captured at time of purchase)
    subscription_type TEXT NOT NULL CHECK (subscription_type IN ('monthly', 'yearly', 'lifetime')),
    subscription_platform TEXT CHECK (subscription_platform IN ('ios', 'android', 'web', 'stripe')),
    subscription_product_id TEXT,

    -- Financial
    gross_amount DECIMAL(10,2) NOT NULL,      -- Full price paid
    app_store_fee DECIMAL(10,2) DEFAULT 0,    -- Apple/Google 15-30% cut
    net_amount DECIMAL(10,2) NOT NULL,        -- Amount after store fees
    commission_amount DECIMAL(10,2) NOT NULL, -- Partner's commission
    commission_percent DECIMAL(5,2) NOT NULL, -- Rate at time of referral (in case it changes)

    -- Commission Status
    commission_status TEXT NOT NULL DEFAULT 'pending' CHECK (commission_status IN (
        'pending',         -- Waiting for subscription to be confirmed (no refund)
        'confirmed',       -- Subscription confirmed, ready for payout
        'paid',            -- Commission paid out
        'cancelled',       -- Subscription refunded/cancelled
        'disputed'         -- Under review
    )),

    -- Lifecycle Tracking
    subscription_started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    subscription_renewed_at TIMESTAMPTZ,      -- For recurring tracking
    subscription_cancelled_at TIMESTAMPTZ,
    commission_confirmed_at TIMESTAMPTZ,      -- After refund window passes

    -- Payout Reference
    payout_id UUID REFERENCES partner_payouts(id),
    paid_at TIMESTAMPTZ,

    -- Metadata
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    -- Prevent duplicate referrals for same user
    UNIQUE(user_id)
);

-- Indexes for reporting
CREATE INDEX IF NOT EXISTS idx_referrals_partner_id ON partner_referrals(partner_id);
CREATE INDEX IF NOT EXISTS idx_referrals_status ON partner_referrals(commission_status);
CREATE INDEX IF NOT EXISTS idx_referrals_created ON partner_referrals(created_at);
CREATE INDEX IF NOT EXISTS idx_referrals_user ON partner_referrals(user_id);


-- ============================================================
-- 4. REFERRAL CODE USAGE TABLE - Track code entries (even without purchase)
-- ============================================================
CREATE TABLE IF NOT EXISTS referral_code_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID REFERENCES auth.users(id),
    referral_code TEXT NOT NULL,
    partner_id UUID REFERENCES partners(id),

    -- Entry Context
    entry_point TEXT NOT NULL CHECK (entry_point IN (
        'onboarding',      -- During initial signup
        'account',         -- Added later in account settings
        'checkout',        -- At purchase time
        'deeplink'         -- Via deeplink/URL
    )),

    -- Validation
    is_valid BOOLEAN NOT NULL DEFAULT false,
    validation_error TEXT,  -- If invalid, why?

    -- Conversion
    converted_to_purchase BOOLEAN DEFAULT false,
    converted_at TIMESTAMPTZ,

    -- Metadata
    device_type TEXT,
    app_version TEXT,
    ip_country TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_code_entries_code ON referral_code_entries(referral_code);
CREATE INDEX IF NOT EXISTS idx_code_entries_user ON referral_code_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_code_entries_partner ON referral_code_entries(partner_id);


-- ============================================================
-- 5. PARTNER STATISTICS VIEW - Aggregated partner performance
-- ============================================================
CREATE OR REPLACE VIEW partner_statistics AS
SELECT
    p.id AS partner_id,
    p.name,
    p.referral_code,
    p.partner_type,
    p.status,
    p.commission_percent,

    -- Referral Stats
    COUNT(DISTINCT pr.id) AS total_referrals,
    COUNT(DISTINCT pr.id) FILTER (WHERE pr.commission_status = 'confirmed') AS confirmed_referrals,
    COUNT(DISTINCT pr.id) FILTER (WHERE pr.commission_status = 'paid') AS paid_referrals,
    COUNT(DISTINCT pr.id) FILTER (WHERE pr.commission_status = 'pending') AS pending_referrals,
    COUNT(DISTINCT pr.id) FILTER (WHERE pr.commission_status = 'cancelled') AS cancelled_referrals,

    -- Financial Stats
    COALESCE(SUM(pr.gross_amount), 0) AS total_gross_revenue,
    COALESCE(SUM(pr.net_amount), 0) AS total_net_revenue,
    COALESCE(SUM(pr.commission_amount), 0) AS total_commissions,
    COALESCE(SUM(pr.commission_amount) FILTER (WHERE pr.commission_status = 'paid'), 0) AS paid_commissions,
    COALESCE(SUM(pr.commission_amount) FILTER (WHERE pr.commission_status IN ('pending', 'confirmed')), 0) AS unpaid_commissions,

    -- Subscription Type Breakdown
    COUNT(*) FILTER (WHERE pr.subscription_type = 'monthly') AS monthly_subs,
    COUNT(*) FILTER (WHERE pr.subscription_type = 'yearly') AS yearly_subs,
    COUNT(*) FILTER (WHERE pr.subscription_type = 'lifetime') AS lifetime_subs,

    -- Time Stats
    MIN(pr.created_at) AS first_referral_at,
    MAX(pr.created_at) AS last_referral_at,

    -- This Month
    COUNT(*) FILTER (WHERE pr.created_at >= date_trunc('month', CURRENT_DATE)) AS referrals_this_month,
    COALESCE(SUM(pr.commission_amount) FILTER (WHERE pr.created_at >= date_trunc('month', CURRENT_DATE)), 0) AS commissions_this_month,

    -- Payout Stats
    (SELECT COUNT(*) FROM partner_payouts pp WHERE pp.partner_id = p.id AND pp.status = 'completed') AS completed_payouts,
    (SELECT COALESCE(SUM(amount), 0) FROM partner_payouts pp WHERE pp.partner_id = p.id AND pp.status = 'completed') AS total_paid_out,

    p.created_at AS partner_since

FROM partners p
LEFT JOIN partner_referrals pr ON p.id = pr.partner_id
GROUP BY p.id;


-- ============================================================
-- 6. MONTHLY PARTNER REPORT VIEW
-- ============================================================
CREATE OR REPLACE VIEW partner_monthly_report AS
SELECT
    p.id AS partner_id,
    p.name,
    p.referral_code,
    date_trunc('month', pr.created_at)::DATE AS month,

    COUNT(*) AS referrals,
    COUNT(*) FILTER (WHERE pr.subscription_type = 'monthly') AS monthly_subs,
    COUNT(*) FILTER (WHERE pr.subscription_type = 'yearly') AS yearly_subs,

    SUM(pr.gross_amount) AS gross_revenue,
    SUM(pr.net_amount) AS net_revenue,
    SUM(pr.commission_amount) AS total_commission,

    AVG(pr.commission_amount) AS avg_commission_per_referral

FROM partners p
JOIN partner_referrals pr ON p.id = pr.partner_id
GROUP BY p.id, p.name, p.referral_code, date_trunc('month', pr.created_at)
ORDER BY month DESC, total_commission DESC;


-- ============================================================
-- 7. HELPER FUNCTIONS
-- ============================================================

-- Function to validate and get partner by referral code
CREATE OR REPLACE FUNCTION validate_referral_code(code TEXT)
RETURNS TABLE (
    is_valid BOOLEAN,
    partner_id UUID,
    partner_name TEXT,
    commission_percent DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        true AS is_valid,
        p.id AS partner_id,
        p.name AS partner_name,
        p.commission_percent
    FROM partners p
    WHERE UPPER(p.referral_code) = UPPER(code)
    AND p.status = 'active';

    -- If no rows returned, return invalid
    IF NOT FOUND THEN
        RETURN QUERY SELECT false, NULL::UUID, NULL::TEXT, NULL::DECIMAL;
    END IF;
END;
$$ LANGUAGE plpgsql;


-- Function to generate unique referral code
CREATE OR REPLACE FUNCTION generate_referral_code(partner_name TEXT)
RETURNS TEXT AS $$
DECLARE
    base_code TEXT;
    final_code TEXT;
    counter INTEGER := 0;
BEGIN
    -- Create base code from name (first 4-6 chars, uppercase, alphanumeric only)
    base_code := UPPER(REGEXP_REPLACE(LEFT(partner_name, 6), '[^A-Z0-9]', '', 'g'));

    -- Ensure minimum length
    IF LENGTH(base_code) < 3 THEN
        base_code := base_code || 'PRO';
    END IF;

    -- Try base code first
    final_code := base_code;

    -- Add numbers until unique
    WHILE EXISTS (SELECT 1 FROM partners WHERE referral_code = final_code) LOOP
        counter := counter + 1;
        final_code := base_code || counter::TEXT;
    END LOOP;

    RETURN final_code;
END;
$$ LANGUAGE plpgsql;


-- Function to calculate commission for a purchase
CREATE OR REPLACE FUNCTION calculate_commission(
    p_partner_id UUID,
    p_gross_amount DECIMAL,
    p_platform TEXT  -- 'ios', 'android', 'web'
)
RETURNS TABLE (
    net_amount DECIMAL,
    app_store_fee DECIMAL,
    commission_amount DECIMAL,
    commission_percent DECIMAL
) AS $$
DECLARE
    v_commission_percent DECIMAL;
    v_store_fee_percent DECIMAL;
    v_net DECIMAL;
    v_store_fee DECIMAL;
    v_commission DECIMAL;
BEGIN
    -- Get partner's commission rate
    SELECT p.commission_percent INTO v_commission_percent
    FROM partners p WHERE p.id = p_partner_id;

    -- Determine app store fee (Apple/Google take 15% for small business, 30% otherwise)
    -- Assuming small business program (15%)
    IF p_platform IN ('ios', 'android') THEN
        v_store_fee_percent := 15.0;
    ELSE
        v_store_fee_percent := 0.0;  -- Web/Stripe - different fees handled elsewhere
    END IF;

    -- Calculate
    v_store_fee := p_gross_amount * (v_store_fee_percent / 100);
    v_net := p_gross_amount - v_store_fee;
    v_commission := v_net * (v_commission_percent / 100);

    RETURN QUERY SELECT v_net, v_store_fee, v_commission, v_commission_percent;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- 8. TRIGGERS FOR UPDATED_AT
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_partners_updated_at
    BEFORE UPDATE ON partners
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_referrals_updated_at
    BEFORE UPDATE ON partner_referrals
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payouts_updated_at
    BEFORE UPDATE ON partner_payouts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- 9. ROW LEVEL SECURITY
-- ============================================================
ALTER TABLE partners ENABLE ROW LEVEL SECURITY;
ALTER TABLE partner_referrals ENABLE ROW LEVEL SECURITY;
ALTER TABLE partner_payouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE referral_code_entries ENABLE ROW LEVEL SECURITY;

-- Partners: Only admins can see/modify
CREATE POLICY "Admin full access to partners" ON partners
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM auth.users
            WHERE auth.users.id = auth.uid()
            AND auth.users.raw_user_meta_data->>'role' = 'admin'
        )
    );

-- Referrals: Admins can see all, users can see their own
CREATE POLICY "Admin full access to referrals" ON partner_referrals
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM auth.users
            WHERE auth.users.id = auth.uid()
            AND auth.users.raw_user_meta_data->>'role' = 'admin'
        )
    );

CREATE POLICY "Users can see own referral" ON partner_referrals
    FOR SELECT USING (user_id = auth.uid());

-- Payouts: Only admins
CREATE POLICY "Admin full access to payouts" ON partner_payouts
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM auth.users
            WHERE auth.users.id = auth.uid()
            AND auth.users.raw_user_meta_data->>'role' = 'admin'
        )
    );

-- Code entries: Public insert for validation, admin read
CREATE POLICY "Anyone can log code entry" ON referral_code_entries
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Admin can read code entries" ON referral_code_entries
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM auth.users
            WHERE auth.users.id = auth.uid()
            AND auth.users.raw_user_meta_data->>'role' = 'admin'
        )
    );


-- ============================================================
-- 10. SAMPLE DATA (Optional - for testing)
-- ============================================================
/*
INSERT INTO partners (name, email, referral_code, partner_type, status, commission_percent, instagram_handle, follower_count)
VALUES
    ('Alex Fitness', 'alex@fitness.com', 'ALEX15', 'influencer', 'active', 15.00, '@alexfitness', 150000),
    ('GymBros Berlin', 'info@gymbros.de', 'GYMBROS', 'gym', 'active', 12.00, '@gymbrosberlin', 25000),
    ('Coach Maria', 'maria@coaching.com', 'MARIA', 'coach', 'active', 15.00, '@coachmaria', 50000),
    ('FitMedia Network', 'partners@fitmedia.com', 'FITMEDIA', 'media', 'active', 10.00, NULL, NULL);
*/


-- ============================================================
-- DONE! Run this script in Supabase SQL Editor
-- ============================================================