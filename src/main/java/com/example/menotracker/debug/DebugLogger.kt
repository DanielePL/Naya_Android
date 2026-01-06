// File: app/src/main/java/com/example/myapplicationtest/debug/DebugLogger.kt
package com.example.menotracker.debug

import android.util.Log

object DebugLogger {
    private const val TAG = "NAYA_DEBUG"

    fun logVideoPathUpdate(location: String, videoPath: String?) {
        Log.e(TAG, "📹 [$location] videoPath updated: $videoPath")
    }

    fun logSetStateUpdate(setId: String, isCompleted: Boolean, videoPath: String?) {
        Log.e(TAG, """
            🎯 SetState Update:
            - SetId: $setId
            - Completed: $isCompleted
            - VideoPath: $videoPath
        """.trimIndent())
    }

    fun logRecomposition(component: String) {
        Log.e(TAG, "🔄 [$component] Recomposing...")
    }
}
