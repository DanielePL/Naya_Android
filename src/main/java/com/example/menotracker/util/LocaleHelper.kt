package com.example.menotracker.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Helper class for managing app locale/language
 */
object LocaleHelper {

    /**
     * Update the app's locale based on the language code
     * @param context The context
     * @param languageCode Language code: "en", "de", "fr", or "system"
     * @return Updated context with new locale
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "en" -> Locale.ENGLISH
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            else -> Locale.getDefault() // "system" or unknown
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Get display name for a language code
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "de" -> "Deutsch"
            "fr" -> "Français"
            else -> "System"
        }
    }

    /**
     * Restart the activity to apply locale changes
     */
    fun restartActivity(activity: Activity) {
        activity.recreate()
    }
}
