package com.example.menotracker.data

import com.example.menotracker.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth) {
            // Configure deep link redirect URL for email verification
            // This tells Supabase to redirect to naya://auth/callback after email verification
            scheme = "naya"
            host = "auth"
        }
        install(Storage)

        httpEngine = OkHttp.create {
            config {
                connectTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
            }
        }
    }
}
