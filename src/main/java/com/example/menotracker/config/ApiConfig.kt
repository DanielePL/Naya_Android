package com.example.menotracker.config

import com.example.menotracker.BuildConfig

/**
 * API Configuration
 *
 * Uses OpenAI API key from BuildConfig
 */
object ApiConfig {
    /**
     * OpenAI API Key
     * Loaded from local.properties: OPENAI_API_KEY
     */
    val OPENAI_API_KEY: String
        get() = BuildConfig.OPENAI_API_KEY
}