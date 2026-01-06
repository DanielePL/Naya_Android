package com.example.menotracker.data.models

import com.google.gson.annotations.SerializedName

/**
 * Chat Message
 * Unified format supporting both old UI code and new API format
 */
data class ChatMessage(
    val id: String = "",
    val content: String,
    val role: String = "user", // "user", "assistant", "system"
    @SerializedName("created_at")
    val createdAt: String = "",
    @Transient
    val metadata: Map<String, Any>? = null, // Transient - not serialized with Gson
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<MessageAttachment>? = null // Nullable because backend doesn't return this field
) {
    // Compatibility with old code that uses isFromUser
    val isFromUser: Boolean
        get() = role == "user"

    fun isUser(): Boolean = role == "user"
    fun isAssistant(): Boolean = role == "assistant"
    fun isSystem(): Boolean = role == "system"
}

data class MessageAttachment(
    val id: String,
    val type: AttachmentType,
    val fileName: String,
    val fileUri: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

enum class AttachmentType {
    DEXA_SCAN,
    MEDICAL_DIAGNOSIS,
    PHYSIO_REPORT,
    IMAGE,
    PDF,
    OTHER
}
