package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.HealthDocument
import com.example.menotracker.data.models.HealthDocumentInsert
import com.example.menotracker.data.models.HealthDocumentType
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Repository für Gesundheitsdokumente (PDFs, Laborberichte, etc.)
 * Speichert Dateien in Supabase Storage und Metadaten in Postgres
 */
object HealthDocumentRepository {
    private const val TAG = "HealthDocumentRepo"
    private const val TABLE_NAME = "health_documents"
    private const val BUCKET_NAME = "health-documents"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _documents = MutableStateFlow<List<HealthDocument>>(emptyList())
    val documents: StateFlow<List<HealthDocument>> = _documents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // UPLOAD DOCUMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Dokument hochladen und speichern
     */
    suspend fun uploadDocument(
        userId: String,
        file: File,
        documentType: HealthDocumentType,
        title: String,
        documentDate: String? = null,
        notes: String? = null
    ): Result<HealthDocument> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _uploadProgress.value = 0f

            Log.d(TAG, "📤 Uploading document: ${file.name}")

            // Determine MIME type
            val mimeType = when {
                file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                file.name.endsWith(".jpg", ignoreCase = true) ||
                file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                else -> "application/octet-stream"
            }

            // Generate unique file path
            val fileExtension = file.extension.ifEmpty { "bin" }
            val uniqueFileName = "${UUID.randomUUID()}.$fileExtension"
            val storagePath = "$userId/$uniqueFileName"

            Log.d(TAG, "📁 Storage path: $storagePath")

            // Upload to Supabase Storage
            val bucket = supabase.storage[BUCKET_NAME]
            val fileBytes = file.readBytes()

            bucket.upload(storagePath, fileBytes, upsert = false)

            _uploadProgress.value = 0.5f

            // Get public URL
            val fileUrl = bucket.publicUrl(storagePath)

            Log.d(TAG, "✅ File uploaded: $fileUrl")

            // Save metadata to database
            val insert = HealthDocumentInsert(
                user_id = userId,
                document_type = documentType.name.lowercase(),
                title = title,
                file_url = fileUrl,
                file_name = file.name,
                file_size_bytes = file.length(),
                mime_type = mimeType,
                document_date = documentDate,
                notes = notes
            )

            val document = supabase.postgrest[TABLE_NAME]
                .insert(insert)
                .decodeSingle<HealthDocument>()

            _uploadProgress.value = 1f

            // Update local state
            _documents.value = listOf(document) + _documents.value

            Log.d(TAG, "✅ Document saved: ${document.id}")
            Result.success(document)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading document", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
            _uploadProgress.value = null
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET DOCUMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Alle Dokumente eines Users laden
     */
    suspend fun getDocuments(userId: String): Result<List<HealthDocument>> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true

            Log.d(TAG, "📥 Loading documents for user: $userId")

            val docs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<HealthDocument>()

            _documents.value = docs

            Log.d(TAG, "✅ Loaded ${docs.size} documents")
            Result.success(docs)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading documents", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Einzelnes Dokument nach ID laden
     */
    suspend fun getDocument(
        userId: String,
        documentId: String
    ): Result<HealthDocument?> = withContext(Dispatchers.IO) {
        try {
            val doc = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("id", documentId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<HealthDocument>()
                .firstOrNull()

            Result.success(doc)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading document", e)
            Result.failure(e)
        }
    }

    /**
     * Dokumente nach Typ filtern
     */
    suspend fun getDocumentsByType(
        userId: String,
        documentType: HealthDocumentType
    ): Result<List<HealthDocument>> = withContext(Dispatchers.IO) {
        try {
            val docs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("document_type", documentType.name.lowercase())
                    }
                    order("document_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<HealthDocument>()

            Result.success(docs)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading documents by type", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE DOCUMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Dokument-Metadaten aktualisieren
     */
    suspend fun updateDocument(
        userId: String,
        documentId: String,
        title: String? = null,
        documentDate: String? = null,
        notes: String? = null
    ): Result<HealthDocument> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Updating document: $documentId")

            supabase.postgrest[TABLE_NAME]
                .update({
                    title?.let { set("title", it) }
                    documentDate?.let { set("document_date", it) }
                    notes?.let { set("notes", it) }
                }) {
                    filter {
                        eq("id", documentId)
                        eq("user_id", userId)
                    }
                }

            // Reload document
            val updated = getDocument(userId, documentId).getOrThrow()
                ?: throw Exception("Document not found after update")

            // Update local state
            _documents.value = _documents.value.map {
                if (it.id == documentId) updated else it
            }

            Log.d(TAG, "✅ Document updated")
            Result.success(updated)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating document", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE DOCUMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Dokument und zugehörige Datei löschen
     */
    suspend fun deleteDocument(
        userId: String,
        documentId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Deleting document: $documentId")

            // Get document first to get file path
            val document = getDocument(userId, documentId).getOrNull()
            if (document == null) {
                Log.w(TAG, "Document not found, nothing to delete")
                return@withContext Result.success(Unit)
            }

            // Delete from storage (extract path from URL)
            try {
                val fileUrl = document.fileUrl
                // Extract path: health-documents/userId/filename
                val pathRegex = Regex("$BUCKET_NAME/(.+)$")
                val match = pathRegex.find(fileUrl)
                val storagePath = match?.groupValues?.getOrNull(1)

                if (storagePath != null) {
                    supabase.storage[BUCKET_NAME].delete(storagePath)
                    Log.d(TAG, "✅ File deleted from storage")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete file from storage: ${e.message}")
                // Continue to delete metadata anyway
            }

            // Delete from database
            supabase.postgrest[TABLE_NAME]
                .delete {
                    filter {
                        eq("id", documentId)
                        eq("user_id", userId)
                    }
                }

            // Update local state
            _documents.value = _documents.value.filter { it.id != documentId }

            Log.d(TAG, "✅ Document deleted")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting document", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Anzahl Dokumente pro Typ
     */
    fun getDocumentCountByType(): Map<HealthDocumentType, Int> {
        return _documents.value
            .groupBy { it.documentTypeEnum }
            .mapValues { it.value.size }
    }

    /**
     * Letztes Dokument eines bestimmten Typs
     */
    fun getLatestDocumentByType(type: HealthDocumentType): HealthDocument? {
        return _documents.value
            .filter { it.documentTypeEnum == type }
            .maxByOrNull { it.documentDate ?: it.createdAt ?: "" }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * State zurücksetzen
     */
    fun clearState() {
        _documents.value = emptyList()
        _isLoading.value = false
        _uploadProgress.value = null
    }

    /**
     * Alle Dokumente neu laden
     */
    suspend fun refresh(userId: String) {
        getDocuments(userId)
    }
}
