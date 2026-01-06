-- ═══════════════════════════════════════════════════════════════
-- UPDATE AI COACH COST RATES - Token-based pricing
-- Run this in Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════

-- Update ai_coach_chat to use token-based pricing (GPT-4o-mini rates)
-- GPT-4o-mini: $0.15/1M input, $0.60/1M output tokens
UPDATE cost_rates
SET
  input_token_cost = 0.00000015,   -- $0.15 per 1M tokens = $0.00000015 per token
  output_token_cost = 0.0000006,   -- $0.60 per 1M tokens = $0.0000006 per token
  base_cost = 0,                    -- Remove fixed base cost
  description = 'AI Coach Chat (GPT-4o-mini): $0.15/1M input, $0.60/1M output tokens',
  updated_at = NOW()
WHERE event_type = 'ai_coach_chat';

-- If the row doesn't exist, insert it
INSERT INTO cost_rates (event_type, input_token_cost, output_token_cost, base_cost, description)
VALUES (
  'ai_coach_chat',
  0.00000015,
  0.0000006,
  0,
  'AI Coach Chat (GPT-4o-mini): $0.15/1M input, $0.60/1M output tokens'
)
ON CONFLICT (event_type) DO UPDATE SET
  input_token_cost = EXCLUDED.input_token_cost,
  output_token_cost = EXCLUDED.output_token_cost,
  base_cost = EXCLUDED.base_cost,
  description = EXCLUDED.description,
  updated_at = NOW();

-- Verify the update
SELECT * FROM cost_rates WHERE event_type = 'ai_coach_chat';

-- ═══════════════════════════════════════════════════════════════
-- COST EXAMPLE CALCULATION:
-- Typical AI Coach message: ~1000 input tokens, ~200 output tokens
-- Cost = (1000 * $0.00000015) + (200 * $0.0000006)
--      = $0.00015 + $0.00012
--      = $0.00027 per message (~$0.27 per 1000 messages)
-- ═══════════════════════════════════════════════════════════════