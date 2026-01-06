-- ============================================================
-- EMPLOYEES / SALARIES TABLE
-- Variable salaries based on net revenue after costs + 15% reserve
-- ============================================================

-- Employees table
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    role TEXT,  -- e.g., 'Developer', 'Marketing', 'Support'
    base_salary DECIMAL(10,2) NOT NULL DEFAULT 0,  -- Ursprünglicher Lohn (Ziel-Lohn)
    revenue_share_percent DECIMAL(5,2) NOT NULL DEFAULT 0,  -- Nettoumsatzbeteiligung in %
    is_active BOOLEAN DEFAULT true,
    start_date DATE DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Monthly salary calculations (stored for history)
CREATE TABLE IF NOT EXISTS salary_calculations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID REFERENCES employees(id) ON DELETE CASCADE,
    month_year TEXT NOT NULL,  -- Format: '2024-01'

    -- Revenue breakdown
    gross_revenue DECIMAL(10,2) DEFAULT 0,
    total_operating_costs DECIMAL(10,2) DEFAULT 0,  -- All fixed + variable costs
    reserve_percent DECIMAL(5,2) DEFAULT 15.0,  -- Rückstellung %
    reserve_amount DECIMAL(10,2) DEFAULT 0,

    -- Net available for salaries
    net_available DECIMAL(10,2) DEFAULT 0,  -- gross - costs - reserve

    -- Employee salary
    revenue_share_percent DECIMAL(5,2) DEFAULT 0,  -- Snapshot of employee's %
    calculated_salary DECIMAL(10,2) DEFAULT 0,  -- Actual salary based on net
    base_salary DECIMAL(10,2) DEFAULT 0,  -- Snapshot of target salary

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for fast lookups
CREATE INDEX IF NOT EXISTS idx_salary_calc_employee ON salary_calculations(employee_id);
CREATE INDEX IF NOT EXISTS idx_salary_calc_month ON salary_calculations(month_year);

-- Enable RLS
ALTER TABLE employees ENABLE ROW LEVEL SECURITY;
ALTER TABLE salary_calculations ENABLE ROW LEVEL SECURITY;

-- Admin-only policies (service role bypasses RLS)
CREATE POLICY "Admin access employees" ON employees FOR ALL USING (true);
CREATE POLICY "Admin access salary_calculations" ON salary_calculations FOR ALL USING (true);

-- ============================================================
-- EXAMPLE: How salary calculation works
-- ============================================================
--
-- Month: January 2025
-- Gross Revenue: $5,000
-- Operating Costs (fixed + variable): $2,000
-- Reserve (15%): $450
--
-- Net Available = $5,000 - $2,000 - $450 = $2,550
--
-- Employee: Daniel
--   Base Salary (target): $3,000
--   Revenue Share: 40%
--   Calculated Salary: $2,550 * 40% = $1,020
--
-- Employee: Max
--   Base Salary (target): $2,000
--   Revenue Share: 25%
--   Calculated Salary: $2,550 * 25% = $637.50
-- ============================================================