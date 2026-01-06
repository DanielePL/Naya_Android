# Supabase Schema Setup

## AI Coach Conversations Schema

Diese Schema erstellt die Tabellen für persistente AI Coach Gespräche.

### Setup Anleitung:

1. **Gehe zu Supabase Dashboard:**
   - https://supabase.com/dashboard
   - Wähle dein Prometheus Projekt

2. **Öffne SQL Editor:**
   - Klicke auf "SQL Editor" in der linken Navigation
   - Klicke auf "New Query"

3. **Kopiere und führe das Schema aus:**
   - Öffne die Datei `ai_coach_schema.sql`
   - Kopiere den **kompletten** SQL Code
   - Füge ihn in den SQL Editor ein
   - Klicke auf "Run" (oder Ctrl/Cmd + Enter)

4. **Bestätige die Erstellung:**
   - Gehe zu "Table Editor"
   - Du solltest jetzt 2 neue Tabellen sehen:
     - `ai_coach_conversations`
     - `ai_coach_messages`

### Was wird erstellt:

**Tables:**
- `ai_coach_conversations` - Speichert Konversations-Threads
- `ai_coach_messages` - Speichert einzelne Nachrichten

**Features:**
- ✅ Automatic timestamps (created_at, updated_at)
- ✅ Soft delete (archived flag)
- ✅ Row Level Security (RLS) - Users können nur ihre eigenen Chats sehen
- ✅ Cascade delete - Messages werden gelöscht wenn Conversation gelöscht wird
- ✅ Indexes für Performance
- ✅ Auto-update trigger für updated_at

**Helper Functions:**
- `generate_conversation_title()` - Generiert Titel aus erster Nachricht
- `get_conversation_summary()` - Holt Konversations-Übersicht mit Metadaten

### Verification:

Nach dem Setup kannst du testen ob alles funktioniert:

```sql
-- Test: Create a conversation
INSERT INTO ai_coach_conversations (user_id, title)
VALUES (auth.uid(), 'Test conversation')
RETURNING *;

-- Test: Get your conversations
SELECT * FROM ai_coach_conversations
WHERE user_id = auth.uid();
```

### Troubleshooting:

**Error: "relation already exists"**
- Die Tabellen existieren bereits. Du kannst das Schema erneut ausführen (es benutzt `IF NOT EXISTS`)

**Error: "permission denied"**
- Stelle sicher dass du als Admin eingeloggt bist im SQL Editor

**Error: "function auth.uid() does not exist"**
- Führe den SQL als "Run as admin" aus (nicht als authenticated user)
