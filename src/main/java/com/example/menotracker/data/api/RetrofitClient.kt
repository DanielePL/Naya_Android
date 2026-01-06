// app/src/main/java/com/example/menotracker/data/api/RetrofitClient.kt

package com.example.menotracker.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // ✅ PRODUCTION: Render.com Deployment
    private const val BASE_URL = "https://naya-backend-69ht.onrender.com/"

    // Development Alternatives:
    // private const val BASE_URL = "http://10.0.2.2:8000/"  // Localhost (Emulator)
    // private const val BASE_URL = "http://192.168.1.34:8000/"  // Local Network (Physical Device)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)  // 2 minutes for connection
        .readTimeout(300, TimeUnit.SECONDS)     // 5 minutes for processing
        .writeTimeout(120, TimeUnit.SECONDS)    // 2 minutes for upload
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}