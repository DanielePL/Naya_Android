package com.example.menotracker.billing

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

/**
 * CompositionLocal for accessing Pro status throughout the app.
 *
 * Usage in any Composable:
 * ```
 * val isPro = LocalProStatus.current
 * if (isPro) {
 *     // Show pro feature
 * } else {
 *     // Show upgrade prompt or limited feature
 * }
 * ```
 */
val LocalProStatus = staticCompositionLocalOf { false }

/**
 * CompositionLocal for accessing the BillingManager.
 * Useful for triggering purchases from anywhere in the app.
 */
val LocalBillingManager = staticCompositionLocalOf<BillingManager?> { null }

/**
 * Provider that wraps your app content and provides Pro status everywhere.
 *
 * Usage in MainActivity:
 * ```
 * ProStatusProvider(context) {
 *     // Your app content
 *     MainScreen()
 * }
 * ```
 */
@Composable
fun ProStatusProvider(
    context: Context,
    content: @Composable () -> Unit
) {
    val billingManager = BillingManager.getInstance(context)
    val isPro by billingManager.isPro.collectAsState()

    CompositionLocalProvider(
        LocalProStatus provides isPro,
        LocalBillingManager provides billingManager
    ) {
        content()
    }
}

/**
 * Extension function to check Pro status and run code conditionally.
 *
 * Usage:
 * ```
 * billingManager.withProStatus { isPro ->
 *     if (isPro) doProThing() else showPaywall()
 * }
 * ```
 */
inline fun BillingManager.withProStatus(block: (Boolean) -> Unit) {
    block(isPro.value)
}

/**
 * Helper object for checking Pro status outside of Compose.
 *
 * Usage:
 * ```
 * if (ProStatus.isPro(context)) {
 *     // Pro feature
 * }
 * ```
 */
object ProStatus {
    fun isPro(context: Context): Boolean {
        return BillingManager.getInstance(context).isPro.value
    }

    fun getProFlow(context: Context): StateFlow<Boolean> {
        return BillingManager.getInstance(context).isPro
    }
}

// NOTE: FreeTierLimits removed - does not match tier model
// Free tier has NO access to VBT, Nutrition, or AI Coach (not limited access)
// Use SubscriptionManager.hasAccess(Feature.X) for feature gating