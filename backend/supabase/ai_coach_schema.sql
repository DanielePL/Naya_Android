-- ============================================================
-- AI Coach Conversations & Messages Schema
-- ============================================================
-- Purpose: Store persistent chat conversations with AI Coach
-- Features:
--   - Conversation threads per user
--   - Message history with role (user/assistant/system)
--   - Metadata for exercise/workout references
--   - Automatic timestamps
-- ============================================================

-- 1. AI Coach Conversations Table
-- Stores conversation threads
CREATE TABLE IF NOT EXISTS ai_coach_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    -- Conversation metadata
    title TEXT,  -- Auto-generated from first user message (e.g., "Squat program discussion")

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Soft delete
    archived BOOLEAN DEFAULT false,

    -- Foreign key
    CONSTRAINT fk_user_id FOREIGN KEY (user_id)
        REFERENCES auth.users(id)
        ON DELETE CASCADE
);

-- 2. AI Coach Messages Table
-- Stores individual messages within conversations
CREATE TABLE IF NOT EXISTS ai_coach_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,

    -- Message content
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,

    -- Optional metadata (JSON)
    -- Examples:
    --   - {"exercise_id": "squat-001", "action": "view_exercise"}
    --   - {"workout_template_id": "uuid", "action": "workout_created"}
    --   - {"tokens_used": 450, "model": "gpt-4-turbo"}
    metadata JSONB,

    -- Timestamp
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Foreign key
    CONSTRAINT fk_conversation_id FOREIGN KEY (conversation_id)
        REFERENCES ai_coach_conversations(id)
        ON DELETE CASCADE
);

-- ============================================================
-- Indexes for Performance
-- ============================================================

-- Fast lookup of user's conversations
CREATE INDEX IF NOT EXISTS idx_conversations_user_id
    ON ai_coach_conversations(user_id);

-- Fast lookup of conversations by update time
CREATE INDEX IF NOT EXISTS idx_conversations_updated
    ON ai_coach_conversations(updated_at DESC);

-- Fast lookup of messages in a conversation
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id
    ON ai_coach_messages(conversation_id);

-- Fast lookup of messages by creation time
CREATE INDEX IF NOT EXISTS idx_messages_created
    ON ai_coach_messages(created_at DESC);

-- ============================================================
-- Automatic Update Timestamp Trigger
-- ============================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_ai_coach_conversation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    -- Update the conversation's updated_at when a new message is added
    UPDATE ai_coach_conversations
    SET updated_at = NOW()
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger on message insert
DROP TRIGGER IF EXISTS trigger_update_conversation_timestamp ON ai_coach_messages;
CREATE TRIGGER trigger_update_conversation_timestamp
    AFTER INSERT ON ai_coach_messages
    FOR EACH ROW
    EXECUTE FUNCTION update_ai_coach_conversation_timestamp();

-- ============================================================
-- Row Level Security (RLS) Policies
-- ============================================================

-- Enable RLS
ALTER TABLE ai_coach_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_coach_messages ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own conversations
CREATE POLICY "Users can view their own conversations"
    ON ai_coach_conversations
    FOR SELECT
    USING (auth.uid() = user_id);

-- Policy: Users can create their own conversations
CREATE POLICY "Users can create their own conversations"
    ON ai_coach_conversations
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Policy: Users can update their own conversations
CREATE POLICY "Users can update their own conversations"
    ON ai_coach_conversations
    FOR UPDATE
    USING (auth.uid() = user_id);

-- Policy: Users can delete their own conversations
CREATE POLICY "Users can delete their own conversations"
    ON ai_coach_conversations
    FOR DELETE
    USING (auth.uid() = user_id);

-- Policy: Users can view messages from their conversations
CREATE POLICY "Users can view messages from their conversations"
    ON ai_coach_messages
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM ai_coach_conversations
            WHERE id = conversation_id
            AND user_id = auth.uid()
        )
    );

-- Policy: Users can create messages in their conversations
CREATE POLICY "Users can create messages in their conversations"
    ON ai_coach_messages
    FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM ai_coach_conversations
            WHERE id = conversation_id
            AND user_id = auth.uid()
        )
    );

-- ============================================================
-- Helper Functions
-- ============================================================

-- Function to auto-generate conversation title from first message
CREATE OR REPLACE FUNCTION generate_conversation_title(
    p_conversation_id UUID
)
RETURNS TEXT AS $$
DECLARE
    first_message TEXT;
    title TEXT;
BEGIN
    -- Get first user message
    SELECT content INTO first_message
    FROM ai_coach_messages
    WHERE conversation_id = p_conversation_id
    AND role = 'user'
    ORDER BY created_at ASC
    LIMIT 1;

    IF first_message IS NULL THEN
        RETURN 'New Conversation';
    END IF;

    -- Truncate to 50 chars and add "..." if longer
    IF LENGTH(first_message) > 50 THEN
        title := SUBSTRING(first_message FROM 1 FOR 50) || '...';
    ELSE
        title := first_message;
    END IF;

    RETURN title;
END;
$$ LANGUAGE plpgsql;

-- Function to get conversation summary
CREATE OR REPLACE FUNCTION get_conversation_summary(
    p_user_id UUID,
    p_limit INT DEFAULT 20
)
RETURNS TABLE (
    conversation_id UUID,
    title TEXT,
    message_count BIGINT,
    last_message TEXT,
    last_message_role TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id AS conversation_id,
        COALESCE(c.title, generate_conversation_title(c.id)) AS title,
        COUNT(m.id) AS message_count,
        (
            SELECT content
            FROM ai_coach_messages
            WHERE conversation_id = c.id
            ORDER BY created_at DESC
            LIMIT 1
        ) AS last_message,
        (
            SELECT role
            FROM ai_coach_messages
            WHERE conversation_id = c.id
            ORDER BY created_at DESC
            LIMIT 1
        ) AS last_message_role,
        c.created_at,
        c.updated_at
    FROM ai_coach_conversations c
    LEFT JOIN ai_coach_messages m ON c.id = m.conversation_id
    WHERE c.user_id = p_user_id
    AND c.archived = false
    GROUP BY c.id
    ORDER BY c.updated_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Example Usage
-- ============================================================
/*

-- Create a new conversation
INSERT INTO ai_coach_conversations (user_id, title)
VALUES ('user-uuid-here', 'Squat program discussion')
RETURNING id;

-- Add messages to conversation
INSERT INTO ai_coach_messages (conversation_id, role, content)
VALUES
    ('conversation-uuid', 'user', 'I want to improve my squat'),
    ('conversation-uuid', 'assistant', 'Great! Let''s build a program for you.');

-- Get user's conversations with summary
SELECT * FROM get_conversation_summary('user-uuid-here', 20);

-- Get all messages in a conversation
SELECT role, content, created_at
FROM ai_coach_messages
WHERE conversation_id = 'conversation-uuid'
ORDER BY created_at ASC;

-- Archive a conversation
UPDATE ai_coach_conversations
SET archived = true
WHERE id = 'conversation-uuid';

*/
