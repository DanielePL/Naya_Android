package com.example.menotracker.community.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.community.data.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatePostState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class CreatePostViewModel : ViewModel() {

    companion object {
        private const val TAG = "CreatePostViewModel"
    }

    private val _state = MutableStateFlow(CreatePostState())
    val state: StateFlow<CreatePostState> = _state.asStateFlow()

    fun createPost(
        userId: String,
        content: String,
        postType: String,
        mediaUris: List<Uri>,
        context: Context
    ) {
        if (content.isBlank()) {
            _state.value = _state.value.copy(error = "Please write something")
            return
        }

        if (content.length > 500) {
            _state.value = _state.value.copy(error = "Post is too long (max 500 characters)")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                // Upload media files first if any
                val uploadedImageUrls = mutableListOf<String>()
                val uploadedVideoUrls = mutableListOf<String>()

                mediaUris.forEachIndexed { index, uri ->
                    val mimeType = context.contentResolver.getType(uri)
                    Log.d(TAG, "📎 Uploading media $index: $mimeType")

                    if (mimeType?.startsWith("video/") == true) {
                        // Upload video
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val videoBytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (videoBytes != null) {
                            // Save to temp file and upload
                            val tempFile = java.io.File(context.cacheDir, "temp_video_$index.mp4")
                            tempFile.writeBytes(videoBytes)

                            val result = FeedRepository.uploadCommunityVideo(
                                userId = userId,
                                videoPath = tempFile.absolutePath,
                                videoIndex = index
                            )

                            result.onSuccess { url ->
                                uploadedVideoUrls.add(url)
                                Log.d(TAG, "✅ Video uploaded: $url")
                            }.onFailure { error ->
                                Log.e(TAG, "❌ Video upload failed: ${error.message}")
                            }

                            tempFile.delete()
                        }
                    } else {
                        // Upload image
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val imageBytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (imageBytes != null) {
                            val result = FeedRepository.uploadCommunityImage(
                                userId = userId,
                                imageBytes = imageBytes,
                                imageIndex = index
                            )

                            result.onSuccess { url ->
                                uploadedImageUrls.add(url)
                                Log.d(TAG, "✅ Image uploaded: $url")
                            }.onFailure { error ->
                                Log.e(TAG, "❌ Image upload failed: ${error.message}")
                            }
                        }
                    }
                }

                // Create the post
                val result = FeedRepository.createCommunityPost(
                    userId = userId,
                    content = content,
                    postType = postType,
                    imageUrls = uploadedImageUrls.ifEmpty { null },
                    videoUrls = uploadedVideoUrls.ifEmpty { null }
                )

                result.onSuccess {
                    Log.d(TAG, "✅ Post created successfully")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }.onFailure { error ->
                    Log.e(TAG, "❌ Post creation failed: ${error.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create post"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }

    fun resetState() {
        _state.value = CreatePostState()
    }
}