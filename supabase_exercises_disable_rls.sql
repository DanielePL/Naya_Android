-- =====================================================
-- DISABLE ROW LEVEL SECURITY FOR EXERCISES TABLE
-- =====================================================
-- This allows the app to read/write exercises without authentication
-- Re-enable RLS when you implement proper authentication!

ALTER TABLE exercises DISABLE ROW LEVEL SECURITY;

-- ✅ RLS disabled - the app can now access exercises without authentication