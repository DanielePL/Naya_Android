package com.example.menotracker.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling billing operations in the UI.
 * Supports 3-tier subscription model: FREE, PREMIUM, ELITE
 */
class BillingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BillingViewModel"
    }

    private val billingManager = BillingManager.getInstance(application)

    // Expose billing manager states
    val subscriptionTier: StateFlow<SubscriptionTier> = billingManager.subscriptionTier
    val isConnected: StateFlow<Boolean> = billingManager.isConnected
    val isLoading: StateFlow<Boolean> = billingManager.isLoading
    val billingError: StateFlow<String?> = billingManager.billingError
    val currentSubscription: StateFlow<SubscriptionInfo?> = billingManager.currentSubscription

    // Premium products ($59/year)
    val premiumYearlyProduct: StateFlow<ProductDetails?> = billingManager.premiumYearlyProduct
    val premiumMonthlyProduct: StateFlow<ProductDetails?> = billingManager.premiumMonthlyProduct

    // Elite products ($99/year)
    val eliteYearlyProduct: StateFlow<ProductDetails?> = billingManager.eliteYearlyProduct
    val eliteMonthlyProduct: StateFlow<ProductDetails?> = billingManager.eliteMonthlyProduct

    // Legacy - for backwards compatibility
    @Deprecated("Use subscriptionTier instead")
    val isPro: StateFlow<Boolean> = billingManager.isPro
    @Deprecated("Use premiumYearlyProduct instead")
    val yearlyProduct: StateFlow<ProductDetails?> = billingManager.premiumYearlyProduct
    @Deprecated("Use premiumMonthlyProduct instead")
    val monthlyProduct: StateFlow<ProductDetails?> = billingManager.premiumMonthlyProduct

    // Selected tier and billing period
    private val _selectedTier = MutableStateFlow(SubscriptionTier.PREMIUM)
    val selectedTier: StateFlow<SubscriptionTier> = _selectedTier.asStateFlow()

    private val _selectedBillingPeriod = MutableStateFlow(BillingPeriod.YEARLY)
    val selectedBillingPeriod: StateFlow<BillingPeriod> = _selectedBillingPeriod.asStateFlow()

    // Legacy - selected plan for backwards compatibility
    private val _selectedPlan = MutableStateFlow(SubscriptionPlan.YEARLY)
    val selectedPlan: StateFlow<SubscriptionPlan> = _selectedPlan.asStateFlow()

    // Purchase result for UI feedback
    private val _purchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val purchaseResult: StateFlow<PurchaseResult?> = _purchaseResult.asStateFlow()

    init {
        // Observe billing errors and convert to purchase results
        viewModelScope.launch {
            billingManager.billingError.collect { error ->
                if (error != null) {
                    _purchaseResult.value = PurchaseResult.Error(error)
                }
            }
        }

        // Observe subscription tier changes
        viewModelScope.launch {
            billingManager.subscriptionTier.collect { tier ->
                if (tier != SubscriptionTier.FREE) {
                    _purchaseResult.value = PurchaseResult.Success(tier)
                }
            }
        }
    }

    /**
     * Select a subscription tier
     */
    fun selectTier(tier: SubscriptionTier) {
        _selectedTier.value = tier
    }

    /**
     * Select a billing period
     */
    fun selectBillingPeriod(period: BillingPeriod) {
        _selectedBillingPeriod.value = period
        // Keep legacy selectedPlan in sync
        _selectedPlan.value = when (period) {
            BillingPeriod.YEARLY -> SubscriptionPlan.YEARLY
            BillingPeriod.MONTHLY -> SubscriptionPlan.MONTHLY
        }
    }

    /**
     * Legacy - select subscription plan
     */
    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
        _selectedBillingPeriod.value = when (plan) {
            SubscriptionPlan.YEARLY -> BillingPeriod.YEARLY
            SubscriptionPlan.MONTHLY -> BillingPeriod.MONTHLY
        }
    }

    /**
     * Get the product details for the currently selected tier and billing period
     */
    fun getSelectedProductDetails(): ProductDetails? {
        return when (_selectedTier.value) {
            SubscriptionTier.FREE -> null
            SubscriptionTier.PREMIUM -> when (_selectedBillingPeriod.value) {
                BillingPeriod.YEARLY -> premiumYearlyProduct.value
                BillingPeriod.MONTHLY -> premiumMonthlyProduct.value
            }
            SubscriptionTier.ELITE -> when (_selectedBillingPeriod.value) {
                BillingPeriod.YEARLY -> eliteYearlyProduct.value
                BillingPeriod.MONTHLY -> eliteMonthlyProduct.value
            }
        }
    }

    /**
     * Launch purchase flow for the selected tier and billing period
     */
    fun purchaseSelectedTier(activity: Activity) {
        val productDetails = getSelectedProductDetails()

        if (productDetails == null) {
            _purchaseResult.value = PurchaseResult.Error("Product not available")
            Log.e(TAG, "Product not available for tier: ${_selectedTier.value}, period: ${_selectedBillingPeriod.value}")
            return
        }

        val offerToken = billingManager.getOfferToken(productDetails)
        if (offerToken == null) {
            _purchaseResult.value = PurchaseResult.Error("No offer available")
            Log.e(TAG, "No offer token for product: ${productDetails.productId}")
            return
        }

        Log.d(TAG, "Launching purchase flow for: ${productDetails.productId}")
        val result = billingManager.launchPurchaseFlow(activity, productDetails, offerToken)

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseResult.value = PurchaseResult.Error("Failed to start purchase: ${result.debugMessage}")
        }
    }

    /**
     * Legacy - Launch purchase flow for the selected plan
     */
    fun purchaseSelectedPlan(activity: Activity) {
        purchaseSelectedTier(activity)
    }

    /**
     * Restore previous purchases
     */
    fun restorePurchases() {
        Log.d(TAG, "Restoring purchases...")
        billingManager.queryPurchases()
    }

    /**
     * Get formatted price for a tier and period
     */
    fun getFormattedPrice(tier: SubscriptionTier, period: BillingPeriod): String {
        val product = when (tier) {
            SubscriptionTier.FREE -> return "$0"
            SubscriptionTier.PREMIUM -> when (period) {
                BillingPeriod.YEARLY -> premiumYearlyProduct.value
                BillingPeriod.MONTHLY -> premiumMonthlyProduct.value
            }
            SubscriptionTier.ELITE -> when (period) {
                BillingPeriod.YEARLY -> eliteYearlyProduct.value
                BillingPeriod.MONTHLY -> eliteMonthlyProduct.value
            }
        }
        return product?.let { billingManager.getFormattedPrice(it) } ?: tier.yearlyPrice
    }

    /**
     * Legacy - Get formatted price for a plan
     */
    fun getFormattedPrice(plan: SubscriptionPlan): String {
        val period = when (plan) {
            SubscriptionPlan.YEARLY -> BillingPeriod.YEARLY
            SubscriptionPlan.MONTHLY -> BillingPeriod.MONTHLY
        }
        return getFormattedPrice(_selectedTier.value, period)
    }

    /**
     * Get price per month for yearly plan
     */
    fun getPricePerMonth(tier: SubscriptionTier): String {
        val yearly = when (tier) {
            SubscriptionTier.FREE -> return "$0"
            SubscriptionTier.PREMIUM -> premiumYearlyProduct.value
            SubscriptionTier.ELITE -> eliteYearlyProduct.value
        } ?: return ""

        val price = yearly.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.priceAmountMicros ?: return ""

        val monthlyMicros = price / 12
        val monthly = monthlyMicros / 1_000_000.0
        val currencyCode = yearly.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.priceCurrencyCode ?: "USD"

        return when (currencyCode) {
            "EUR" -> "€%.2f".format(monthly)
            "USD" -> "$%.2f".format(monthly)
            "GBP" -> "£%.2f".format(monthly)
            else -> "%.2f %s".format(monthly, currencyCode)
        }
    }

    /**
     * Legacy - Get price per month for yearly plan
     */
    fun getYearlyPricePerMonth(): String = getPricePerMonth(_selectedTier.value)

    /**
     * Calculate savings percentage for yearly vs monthly
     */
    fun getSavingsPercent(tier: SubscriptionTier): Int {
        val yearlyProduct = when (tier) {
            SubscriptionTier.FREE -> return 0
            SubscriptionTier.PREMIUM -> premiumYearlyProduct.value
            SubscriptionTier.ELITE -> eliteYearlyProduct.value
        }

        val monthlyProduct = when (tier) {
            SubscriptionTier.FREE -> return 0
            SubscriptionTier.PREMIUM -> premiumMonthlyProduct.value
            SubscriptionTier.ELITE -> eliteMonthlyProduct.value
        }

        val yearlyPrice = yearlyProduct?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.priceAmountMicros ?: return 0

        val monthlyPrice = monthlyProduct?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.priceAmountMicros ?: return 0

        val monthlyTotal = monthlyPrice * 12

        if (monthlyTotal == 0L) return 0

        val savings = ((monthlyTotal - yearlyPrice).toDouble() / monthlyTotal * 100).toInt()
        return savings.coerceIn(0, 100)
    }

    /**
     * Legacy - Calculate savings percentage for yearly vs monthly
     */
    fun getYearlySavingsPercent(): Int = getSavingsPercent(_selectedTier.value)

    /**
     * Clear purchase result after handling
     */
    fun clearPurchaseResult() {
        _purchaseResult.value = null
    }

    /**
     * Clear error state
     */
    fun clearError() {
        billingManager.clearError()
        _purchaseResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Don't end connection here - it's a singleton that should persist
    }
}

/**
 * Billing period options
 */
enum class BillingPeriod {
    YEARLY,
    MONTHLY
}

/**
 * Subscription plans available (Legacy - use BillingPeriod instead)
 */
enum class SubscriptionPlan {
    YEARLY,
    MONTHLY
}

/**
 * Result of a purchase attempt
 */
sealed class PurchaseResult {
    data class Success(val tier: SubscriptionTier = SubscriptionTier.PREMIUM) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    object Canceled : PurchaseResult()
}