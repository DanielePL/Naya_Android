"""
AI Coach Conversation Manager
Handles persistent chat storage and retrieval with Supabase
"""

import os
from typing import Dict, List, Optional
from datetime import datetime
from supabase import create_client, Client


class ConversationManager:
    """Manages AI Coach conversations in Supabase"""

    def __init__(self):
        """Initialize Supabase client"""
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise ValueError(
                "SUPABASE_URL and SUPABASE_KEY environment variables required"
            )

        self.client: Client = create_client(supabase_url, supabase_key)

    def create_conversation(self, user_id: str, title: Optional[str] = None) -> Dict:
        """
        Create a new conversation

        Args:
            user_id: UUID of the user
            title: Optional conversation title

        Returns:
            Dict with conversation_id and created_at
        """
        try:
            data = {
                "user_id": user_id,
                "title": title,
                "archived": False
            }

            result = self.client.table("ai_coach_conversations")\
                .insert(data)\
                .execute()

            if result.data and len(result.data) > 0:
                conversation = result.data[0]
                return {
                    "success": True,
                    "conversation_id": conversation["id"],
                    "created_at": conversation["created_at"]
                }
            else:
                return {
                    "success": False,
                    "error": "Failed to create conversation"
                }

        except Exception as e:
            print(f"❌ Error creating conversation: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def add_message(
        self,
        conversation_id: str,
        role: str,
        content: str,
        metadata: Optional[Dict] = None
    ) -> Dict:
        """
        Add a message to a conversation

        Args:
            conversation_id: UUID of the conversation
            role: 'user', 'assistant', or 'system'
            content: Message text
            metadata: Optional JSON metadata

        Returns:
            Dict with message_id and created_at
        """
        try:
            data = {
                "conversation_id": conversation_id,
                "role": role,
                "content": content,
                "metadata": metadata or {}
            }

            result = self.client.table("ai_coach_messages")\
                .insert(data)\
                .execute()

            if result.data and len(result.data) > 0:
                message = result.data[0]
                return {
                    "success": True,
                    "message_id": message["id"],
                    "created_at": message["created_at"]
                }
            else:
                return {
                    "success": False,
                    "error": "Failed to add message"
                }

        except Exception as e:
            print(f"❌ Error adding message: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def get_conversation_messages(
        self,
        conversation_id: str,
        limit: int = 100
    ) -> List[Dict]:
        """
        Get all messages in a conversation

        Args:
            conversation_id: UUID of the conversation
            limit: Max number of messages to return

        Returns:
            List of message dicts with role, content, created_at
        """
        try:
            result = self.client.table("ai_coach_messages")\
                .select("id, role, content, metadata, created_at")\
                .eq("conversation_id", conversation_id)\
                .order("created_at", desc=False)\
                .limit(limit)\
                .execute()

            if result.data:
                return result.data
            else:
                return []

        except Exception as e:
            print(f"❌ Error getting messages: {str(e)}")
            return []

    def get_recent_messages(
        self,
        conversation_id: str,
        count: int = 20
    ) -> List[Dict]:
        """
        Get the most recent N messages from a conversation
        Useful for building context for AI

        Args:
            conversation_id: UUID of the conversation
            count: Number of recent messages

        Returns:
            List of recent messages (oldest first for context)
        """
        try:
            result = self.client.table("ai_coach_messages")\
                .select("role, content, metadata")\
                .eq("conversation_id", conversation_id)\
                .order("created_at", desc=True)\
                .limit(count)\
                .execute()

            if result.data:
                # Reverse to get chronological order (oldest first)
                return list(reversed(result.data))
            else:
                return []

        except Exception as e:
            print(f"❌ Error getting recent messages: {str(e)}")
            return []

    def get_user_conversations(
        self,
        user_id: str,
        include_archived: bool = False,
        limit: int = 20
    ) -> List[Dict]:
        """
        Get all conversations for a user

        Args:
            user_id: UUID of the user
            include_archived: Whether to include archived conversations
            limit: Max number of conversations to return

        Returns:
            List of conversation dicts with metadata
        """
        try:
            query = self.client.table("ai_coach_conversations")\
                .select("*")\
                .eq("user_id", user_id)

            if not include_archived:
                query = query.eq("archived", False)

            result = query.order("updated_at", desc=True)\
                .limit(limit)\
                .execute()

            if result.data:
                # For each conversation, get message count and last message
                conversations = []
                for conv in result.data:
                    # Get message count
                    msg_result = self.client.table("ai_coach_messages")\
                        .select("id", count="exact")\
                        .eq("conversation_id", conv["id"])\
                        .execute()

                    message_count = msg_result.count if msg_result.count else 0

                    # Get last message
                    last_msg_result = self.client.table("ai_coach_messages")\
                        .select("content, role, created_at")\
                        .eq("conversation_id", conv["id"])\
                        .order("created_at", desc=True)\
                        .limit(1)\
                        .execute()

                    last_message = None
                    last_message_role = None
                    if last_msg_result.data and len(last_msg_result.data) > 0:
                        last_message = last_msg_result.data[0]["content"]
                        last_message_role = last_msg_result.data[0]["role"]

                    conversations.append({
                        "id": conv["id"],
                        "title": conv.get("title") or self._generate_title(conv["id"]),
                        "message_count": message_count,
                        "last_message": last_message,
                        "last_message_role": last_message_role,
                        "created_at": conv["created_at"],
                        "updated_at": conv["updated_at"],
                        "archived": conv["archived"]
                    })

                return conversations
            else:
                return []

        except Exception as e:
            print(f"❌ Error getting user conversations: {str(e)}")
            return []

    def update_conversation_title(
        self,
        conversation_id: str,
        title: str
    ) -> Dict:
        """
        Update conversation title

        Args:
            conversation_id: UUID of the conversation
            title: New title

        Returns:
            Dict with success status
        """
        try:
            result = self.client.table("ai_coach_conversations")\
                .update({"title": title})\
                .eq("id", conversation_id)\
                .execute()

            return {
                "success": True,
                "conversation_id": conversation_id
            }

        except Exception as e:
            print(f"❌ Error updating title: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def archive_conversation(
        self,
        conversation_id: str
    ) -> Dict:
        """
        Archive a conversation (soft delete)

        Args:
            conversation_id: UUID of the conversation

        Returns:
            Dict with success status
        """
        try:
            result = self.client.table("ai_coach_conversations")\
                .update({"archived": True})\
                .eq("id", conversation_id)\
                .execute()

            return {
                "success": True,
                "conversation_id": conversation_id
            }

        except Exception as e:
            print(f"❌ Error archiving conversation: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def delete_conversation(
        self,
        conversation_id: str
    ) -> Dict:
        """
        Permanently delete a conversation and all its messages

        Args:
            conversation_id: UUID of the conversation

        Returns:
            Dict with success status
        """
        try:
            # Messages will be deleted automatically via CASCADE
            result = self.client.table("ai_coach_conversations")\
                .delete()\
                .eq("id", conversation_id)\
                .execute()

            return {
                "success": True,
                "conversation_id": conversation_id
            }

        except Exception as e:
            print(f"❌ Error deleting conversation: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def _generate_title(self, conversation_id: str) -> str:
        """
        Generate a title from the first user message

        Args:
            conversation_id: UUID of the conversation

        Returns:
            Generated title string
        """
        try:
            result = self.client.table("ai_coach_messages")\
                .select("content")\
                .eq("conversation_id", conversation_id)\
                .eq("role", "user")\
                .order("created_at", desc=False)\
                .limit(1)\
                .execute()

            if result.data and len(result.data) > 0:
                first_message = result.data[0]["content"]
                # Truncate to 50 chars
                if len(first_message) > 50:
                    return first_message[:50] + "..."
                return first_message
            else:
                return "New Conversation"

        except Exception as e:
            print(f"❌ Error generating title: {str(e)}")
            return "New Conversation"
