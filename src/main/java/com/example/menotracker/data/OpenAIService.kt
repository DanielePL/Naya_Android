package com.example.menotracker.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// ═══════════════════════════════════════════════════════════════
// OpenAI REQUEST DATA CLASSES
// ═══════════════════════════════════════════════════════════════

data class OpenAIRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.7
)

data class OpenAIMessage(
    val role: String,
    val content: Any  // Can be String or List<OpenAIContent>
)

data class OpenAIContent(
    val type: String,          // "text" or "image_url"
    val text: String? = null,  // For type="text"
    @SerializedName("image_url") val imageUrl: OpenAIImageUrl? = null  // For type="image_url"
)

data class OpenAIImageUrl(
    val url: String  // "data:image/jpeg;base64,{base64_image}"
)

// ═══════════════════════════════════════════════════════════════
// OpenAI RESPONSE DATA CLASSES
// ═══════════════════════════════════════════════════════════════

data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage
)

data class OpenAIChoice(
    val index: Int,
    val message: OpenAIResponseMessage,
    @SerializedName("finish_reason") val finishReason: String
)

data class OpenAIResponseMessage(
    val role: String,
    val content: String
)

data class OpenAIUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// ═══════════════════════════════════════════════════════════════
// RETROFIT SERVICE INTERFACE
// ═══════════════════════════════════════════════════════════════

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,  // "Bearer sk-..."
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: OpenAIRequest
    ): OpenAIResponse

    companion object {
        private const val BASE_URL = "https://api.openai.com/"

        fun create(): OpenAIService {
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                // ✅ VISION API needs longer timeouts (large base64 images)
                .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request()
                    android.util.Log.d("OpenAI", "🌐 URL: ${request.url}")
                    android.util.Log.d("OpenAI", "🔑 Auth: ${request.header("Authorization")?.take(30)}...")

                    val response = chain.proceed(request)
                    android.util.Log.d("OpenAI", "✅ Response: ${response.code}")

                    if (!response.isSuccessful) {
                        val errorBody = response.peekBody(2048).string()
                        android.util.Log.e("OpenAI", "❌ Error: $errorBody")
                    }
                    response
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(OpenAIService::class.java)
        }
    }
}