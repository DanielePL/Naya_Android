package com.example.menotracker.community.util

/**
 * Feature flags for Community feature.
 *
 * Set ENABLED to false to completely disable community features without code changes.
 * This provides an emergency kill switch if any issues occur after deployment.
 */
object CommunityFeatureFlag {

    // ═══════════════════════════════════════════════════════════════════════
    // MASTER SWITCH
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Master switch for entire Community feature.
     * Set to false to hide all community functionality.
     */
    const val ENABLED = true  // ENABLED for testing

    // ═══════════════════════════════════════════════════════════════════════
    // INDIVIDUAL FEATURE TOGGLES
    // ═══════════════════════════════════════════════════════════════════════

    /** Feed with posts from followed users */
    const val FEED_ENABLED = true

    /** Discover tab with public posts */
    const val DISCOVER_ENABLED = true

    /** Exercise leaderboards */
    const val LEADERBOARD_ENABLED = true

    /** Challenges including Max Out Friday */
    const val CHALLENGES_ENABLED = true

    /** Max Out Friday weekly challenge */
    const val MAX_OUT_FRIDAY_ENABLED = true

    /** Share workout dialog after completing workout */
    const val SHARE_DIALOG_ENABLED = true

    // ═══════════════════════════════════════════════════════════════════════
    // DEBUG FLAGS
    // ═══════════════════════════════════════════════════════════════════════

    /** Show debug info in UI */
    const val SHOW_DEBUG_INFO = false

    /** Enable verbose logging */
    const val VERBOSE_LOGGING = true
}