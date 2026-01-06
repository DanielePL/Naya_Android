package com.example.menotracker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BillingManager handles all Google Play Billing operations for Naya.
 *
 * Product IDs must match exactly what you create in Google Play Console:
 * (Note: Product IDs use legacy "prometheus_" prefix for Play Store compatibility)
 *
 * PREMIUM ($59/year): VBT OR Nutrition
 * - naya_premium_yearly: $59/year
 * - naya_premium_monthly: $5.99/month
 *
 * ELITE ($99/year): Everything + AI Coach + Physical Coach
 * - naya_elite_yearly: $99/year
 * - naya_elite_monthly: $9.90/month
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // Premium Product IDs ($59/year - VBT OR Nutrition)
        const val PRODUCT_PREMIUM_YEARLY = "prometheus_premium_yearly"
        const val PRODUCT_PREMIUM_MONTHLY = "prometheus_premium_monthly"

        // Elite Product IDs ($99/year - Everything)
        const val PRODUCT_ELITE_YEARLY = "prometheus_elite_yearly"
        const val PRODUCT_ELITE_MONTHLY = "prometheus_elite_monthly"

        // Legacy - keep for migration
        @Deprecated("Use PRODUCT_PREMIUM_YEARLY instead")
        const val PRODUCT_YEARLY = "prometheus_pro_yearly"
        @Deprecated("Use PRODUCT_PREMIUM_MONTHLY instead")
        const val PRODUCT_MONTHLY = "prometheus_pro_monthly"

        // Singleton instance
        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Billing client
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Current subscription tier
    private val _subscriptionTier = MutableStateFlow(SubscriptionTier.FREE)
    val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()

    // Legacy Pro status (for backwards compatibility)
    // Maps subscription tier to boolean: true if user has any paid subscription
    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    // Current subscription info
    private val _currentSubscription = MutableStateFlow<SubscriptionInfo?>(null)
    val currentSubscription: StateFlow<SubscriptionInfo?> = _currentSubscription.asStateFlow()

    // Premium Products ($59/year)
    private val _premiumYearlyProduct = MutableStateFlow<ProductDetails?>(null)
    val premiumYearlyProduct: StateFlow<ProductDetails?> = _premiumYearlyProduct.asStateFlow()

    private val _premiumMonthlyProduct = MutableStateFlow<ProductDetails?>(null)
    val premiumMonthlyProduct: StateFlow<ProductDetails?> = _premiumMonthlyProduct.asStateFlow()

    // Elite Products ($99/year)
    private val _eliteYearlyProduct = MutableStateFlow<ProductDetails?>(null)
    val eliteYearlyProduct: StateFlow<ProductDetails?> = _eliteYearlyProduct.asStateFlow()

    private val _eliteMonthlyProduct = MutableStateFlow<ProductDetails?>(null)
    val eliteMonthlyProduct: StateFlow<ProductDetails?> = _eliteMonthlyProduct.asStateFlow()

    // Legacy - keep for migration
    @Deprecated("Use premiumYearlyProduct instead")
    val yearlyProduct: StateFlow<ProductDetails?> = _premiumYearlyProduct

    @Deprecated("Use premiumMonthlyProduct instead")
    val monthlyProduct: StateFlow<ProductDetails?> = _premiumMonthlyProduct

    // Error state
    private val _billingError = MutableStateFlow<String?>(null)
    val billingError: StateFlow<String?> = _billingError.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        startConnection()
    }

    /**
     * Start connection to Google Play Billing
     */
    fun startConnection() {
        if (billingClient.isReady) {
            Log.d(TAG, "BillingClient already connected")
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient connected successfully")
                    _isConnected.value = true
                    _billingError.value = null

                    // Query available products and existing purchases
                    queryProducts()
                    queryPurchases()
                } else {
                    Log.e(TAG, "BillingClient connection failed: ${billingResult.debugMessage}")
                    _isConnected.value = false
                    _billingError.value = "Billing connection failed: ${billingResult.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "BillingClient disconnected")
                _isConnected.value = false
                // Try to reconnect
                startConnection()
            }
        })
    }

    /**
     * Query available subscription products from Google Play
     */
    private fun queryProducts() {
        val productList = listOf(
            // Premium products
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            // Elite products
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ELITE_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ELITE_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            // Legacy products (for migration)
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { product ->
                    when (product.productId) {
                        PRODUCT_PREMIUM_YEARLY -> {
                            _premiumYearlyProduct.value = product
                            Log.d(TAG, "Premium yearly product loaded: ${product.name}")
                        }
                        PRODUCT_PREMIUM_MONTHLY -> {
                            _premiumMonthlyProduct.value = product
                            Log.d(TAG, "Premium monthly product loaded: ${product.name}")
                        }
                        PRODUCT_ELITE_YEARLY -> {
                            _eliteYearlyProduct.value = product
                            Log.d(TAG, "Elite yearly product loaded: ${product.name}")
                        }
                        PRODUCT_ELITE_MONTHLY -> {
                            _eliteMonthlyProduct.value = product
                            Log.d(TAG, "Elite monthly product loaded: ${product.name}")
                        }
                        // Legacy - map to premium
                        PRODUCT_YEARLY -> {
                            if (_premiumYearlyProduct.value == null) {
                                _premiumYearlyProduct.value = product
                            }
                        }
                        PRODUCT_MONTHLY -> {
                            if (_premiumMonthlyProduct.value == null) {
                                _premiumMonthlyProduct.value = product
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
                _billingError.value = "Failed to load products"
            }
        }
    }

    /**
     * Query existing purchases to restore subscription tier
     */
    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchasesList)
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Process purchased subscriptions and determine tier
     */
    private fun processPurchases(purchases: List<Purchase>) {
        var highestTier = SubscriptionTier.FREE

        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Verify and acknowledge if not already done
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }

                // Determine which tier was purchased
                val productId = purchase.products.firstOrNull() ?: return@forEach
                val tier = when (productId) {
                    PRODUCT_ELITE_YEARLY, PRODUCT_ELITE_MONTHLY -> SubscriptionTier.ELITE
                    PRODUCT_PREMIUM_YEARLY, PRODUCT_PREMIUM_MONTHLY,
                    PRODUCT_YEARLY, PRODUCT_MONTHLY -> SubscriptionTier.PREMIUM // Legacy mapped to Premium
                    else -> SubscriptionTier.FREE
                }

                // Keep the highest tier if user has multiple subscriptions
                if (tier.ordinal > highestTier.ordinal) {
                    highestTier = tier
                    _currentSubscription.value = SubscriptionInfo(
                        productId = productId,
                        purchaseToken = purchase.purchaseToken,
                        purchaseTime = purchase.purchaseTime,
                        isAutoRenewing = purchase.isAutoRenewing,
                        tier = tier
                    )
                    Log.d(TAG, "Active subscription found: $productId -> $tier")
                }
            }
        }

        _subscriptionTier.value = highestTier
        _isPro.value = highestTier != SubscriptionTier.FREE // Update legacy isPro flag
        SubscriptionManager.setTier(highestTier) // Sync with SubscriptionManager
        Log.d(TAG, "Subscription tier: $highestTier, isPro: ${_isPro.value}")
    }

    /**
     * Acknowledge a purchase (required within 3 days)
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Launch the purchase flow for a subscription
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): BillingResult {
        _isLoading.value = true

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Called when a purchase is updated (completed, canceled, etc.)
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _isLoading.value = false

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "Purchase successful")
                purchases?.let { processPurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase canceled by user")
                _billingError.value = null // Not an error, user chose to cancel
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned")
                queryPurchases() // Refresh purchase state
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _billingError.value = "Purchase failed: ${billingResult.debugMessage}"
            }
        }
    }

    /**
     * Get the offer token for a subscription product
     */
    fun getOfferToken(productDetails: ProductDetails): String? {
        return productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
    }

    /**
     * Get formatted price for display
     */
    fun getFormattedPrice(productDetails: ProductDetails): String {
        return productDetails.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice ?: ""
    }

    /**
     * Get billing period description
     */
    fun getBillingPeriod(productDetails: ProductDetails): String {
        val period = productDetails.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.billingPeriod ?: return ""

        return when (period) {
            "P1M" -> "month"
            "P1Y" -> "year"
            "P3M" -> "3 months"
            "P6M" -> "6 months"
            else -> period
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _billingError.value = null
    }

    /**
     * Clean up resources
     */
    fun endConnection() {
        billingClient.endConnection()
        _isConnected.value = false
    }
}

/**
 * Data class to hold subscription information
 */
data class SubscriptionInfo(
    val productId: String,
    val purchaseToken: String,
    val purchaseTime: Long,
    val isAutoRenewing: Boolean,
    val tier: SubscriptionTier = SubscriptionTier.FREE
)