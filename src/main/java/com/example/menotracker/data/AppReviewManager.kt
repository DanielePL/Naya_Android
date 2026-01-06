package com.example.menotracker.data

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * AppReviewManager - Handles Google Play In-App Review flow.
 *
 * Uses the "Happiness Gate" pattern:
 * 1. First ask if user is happy (custom dialog)
 * 2. Happy users → In-App Review (stays in app)
 * 3. Unhappy users → Feedback form (valuable insights)
 *
 * Trigger points:
 * - After 5th completed workout (first prompt)
 * - After every 10 additional workouts if dismissed
 * - Minimum 7 days between prompts
 * - Maximum 3 dismissals, then stop asking
 */
class AppReviewManager(
    private val activity: Activity,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val TAG = "AppReviewManager"
    }

    private val reviewManager = ReviewManagerFactory.create(activity)

    /**
     * Check if we should show the review prompt.
     * Call this after workout completion.
     */
    suspend fun shouldShowReviewPrompt(): Boolean {
        val reviewState = settingsDataStore.reviewState.first()
        return reviewState.shouldShowReviewPrompt()
    }

    /**
     * Launch the Google Play In-App Review flow.
     * The user stays in the app - a bottom sheet appears.
     *
     * Note: Google doesn't guarantee the dialog will show (quota limits).
     * Don't rely on any callback to confirm the user rated.
     */
    suspend fun launchInAppReview() {
        try {
            Log.d(TAG, "Requesting In-App Review flow...")

            // Request review info from Google Play
            val reviewInfo = reviewManager.requestReviewFlow().await()

            // Launch the review flow
            reviewManager.launchReviewFlow(activity, reviewInfo).await()

            // Mark as rated (we can't know if they actually rated, but we assume good intent)
            settingsDataStore.setHasRatedApp(true)

            Log.d(TAG, "In-App Review flow completed")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch In-App Review: ${e.message}")
            // Silent fail - don't bother the user
        }
    }

    /**
     * Record that the user dismissed the prompt.
     * We'll ask again later (with increasing intervals).
     */
    suspend fun onPromptDismissed() {
        settingsDataStore.incrementReviewPromptDismissed()
        settingsDataStore.setLastReviewPromptTimestamp(System.currentTimeMillis())
        Log.d(TAG, "Review prompt dismissed")
    }

    /**
     * Record that the prompt was shown (for timing purposes).
     */
    suspend fun onPromptShown() {
        settingsDataStore.setLastReviewPromptTimestamp(System.currentTimeMillis())
        Log.d(TAG, "Review prompt shown")
    }

    /**
     * User gave negative feedback - redirect to feedback form.
     * This is more valuable than a bad review!
     */
    suspend fun onNegativeFeedback() {
        // Mark as "rated" so we don't ask again
        settingsDataStore.setHasRatedApp(true)
        Log.d(TAG, "User gave negative feedback - redirecting to feedback form")
        // The UI will handle showing the feedback dialog
    }

    /**
     * Increment completed workouts counter.
     * Call this when a workout is completed.
     */
    suspend fun onWorkoutCompleted() {
        settingsDataStore.incrementCompletedWorkouts()
    }
}
