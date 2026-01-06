-- ============================================================
-- PARTNER SYSTEM TABLES
-- For Stripe webhook integration and automatic commission tracking
-- ============================================================

-- Partners table (if not exists)
CREATE TABLE IF NOT EXISTS partners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    referral_code TEXT NOT NULL UNIQUE,
    commission_percent DECIMAL(5,2) NOT NULL DEFAULT 15,
    status TEXT DEFAULT 'active', -- active, paused, terminated
    payout_method TEXT, -- paypal, bank_transfer, wise
    payout_details JSONB DEFAULT '{}', -- Contains password_hash and payment info
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Partner referrals - tracks every commission-eligible transaction
CREATE TABLE IF NOT EXISTS partner_referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id UUID REFERENCES partners(id) ON DELETE CASCADE,
    user_id UUID, -- Prometheus user who used the code

    -- Stripe data
    stripe_customer_id TEXT,
    stripe_subscription_id TEXT,
    stripe_checkout_session_id TEXT,
    stripe_invoice_id TEXT,
    customer_email TEXT,

    -- Transaction details
    subscription_type TEXT, -- monthly, yearly, recurring
    gross_amount DECIMAL(10,2) NOT NULL,
    currency TEXT DEFAULT 'USD',
    commission_percent DECIMAL(5,2) NOT NULL,
    commission_amount DECIMAL(10,2) NOT NULL,

    -- Status tracking
    commission_status TEXT DEFAULT 'pending', -- pending, confirmed, paid, cancelled
    confirmed_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    payout_id UUID, -- Reference to partner_payouts when paid

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Referral code entries - tracks every time a code is used (for conversion rate)
CREATE TABLE IF NOT EXISTS referral_code_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id UUID REFERENCES partners(id) ON DELETE CASCADE,
    referral_code TEXT NOT NULL,
    user_id UUID,
    converted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Partner payouts - monthly payout records
CREATE TABLE IF NOT EXISTS partner_payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id UUID REFERENCES partners(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    referral_count INTEGER DEFAULT 0,
    gross_amount DECIMAL(10,2) DEFAULT 0,
    amount DECIMAL(10,2) NOT NULL, -- Net payout amount
    currency TEXT DEFAULT 'USD',
    status TEXT DEFAULT 'pending', -- pending, processing, completed, failed
    payout_method TEXT,
    payout_reference TEXT, -- PayPal transaction ID, bank reference, etc.
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_partner_referrals_partner ON partner_referrals(partner_id);
CREATE INDEX IF NOT EXISTS idx_partner_referrals_subscription ON partner_referrals(stripe_subscription_id);
CREATE INDEX IF NOT EXISTS idx_partner_referrals_status ON partner_referrals(commission_status);
CREATE INDEX IF NOT EXISTS idx_partner_referrals_created ON partner_referrals(created_at);
CREATE INDEX IF NOT EXISTS idx_referral_entries_partner ON referral_code_entries(partner_id);
CREATE INDEX IF NOT EXISTS idx_partner_payouts_partner ON partner_payouts(partner_id);
CREATE INDEX IF NOT EXISTS idx_partners_code ON partners(referral_code);

-- Enable RLS
ALTER TABLE partners ENABLE ROW LEVEL SECURITY;
ALTER TABLE partner_referrals ENABLE ROW LEVEL SECURITY;
ALTER TABLE referral_code_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE partner_payouts ENABLE ROW LEVEL SECURITY;

-- Policies (service role bypasses RLS)
CREATE POLICY "Service access partners" ON partners FOR ALL USING (true);
CREATE POLICY "Service access partner_referrals" ON partner_referrals FOR ALL USING (true);
CREATE POLICY "Service access referral_code_entries" ON referral_code_entries FOR ALL USING (true);
CREATE POLICY "Service access partner_payouts" ON partner_payouts FOR ALL USING (true);

-- ============================================================
-- EXAMPLE: Create a test partner
-- ============================================================
-- INSERT INTO partners (name, email, referral_code, commission_percent, status, payout_method, payout_details)
-- VALUES (
--     'Daniel Pauli',
--     'danielepauli@gmail.com',
--     'DANIEL20',
--     20,
--     'active',
--     'paypal',
--     '{"paypal_email": "danielepauli@gmail.com", "password_hash": "61ba9075e774c6f164a288aed462aec9b25faea6910d2dee0262e4d82bcb14ab"}'::jsonb
-- );