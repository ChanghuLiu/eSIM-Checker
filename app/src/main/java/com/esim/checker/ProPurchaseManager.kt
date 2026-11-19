package com.esim.checker

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/** Google Play non-consumable lifetime unlock. Product must exist in Play Console. */
class ProPurchaseManager(
    context: Context,
    private val onStateChanged: (ProPurchaseState) -> Unit,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private var productDetails: ProductDetails? = null
    private var isConnected = false
    private var isConnecting = false
    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(PurchasesUpdatedListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::processPurchase)
                BillingClient.BillingResponseCode.USER_CANCELED -> onStateChanged(currentState())
                else -> onStateChanged(currentState().copy(message = result.debugMessage))
            }
        })
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (isConnected || billingClient.isReady) {
            refresh()
            return
        }
        if (isConnecting) return
        isConnecting = true
        onStateChanged(currentState().copy(loading = true))
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                isConnecting = false
                isConnected = result.responseCode == BillingClient.BillingResponseCode.OK
                if (isConnected) refresh()
                else onStateChanged(currentState().copy(loading = false, message = result.debugMessage))
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                isConnecting = false
                onStateChanged(currentState().copy(loading = false))
            }
        })
    }

    fun refresh() {
        if (!billingClient.isReady) {
            start()
            return
        }
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList.firstOrNull()
            }
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            ) { purchaseResult, purchases ->
                if (purchaseResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val owned = purchases.any {
                        PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    preferences.edit().putBoolean(KEY_OWNED, owned).apply()
                    purchases.filter { PRODUCT_ID in it.products }.forEach(::processPurchase)
                }
                onStateChanged(currentState().copy(loading = false))
            }
        }
    }

    fun buy(activity: Activity) {
        val details = productDetails
        val offerToken = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
        if (details == null || offerToken == null) {
            onStateChanged(currentState().copy(message = "Product is not available on Google Play yet."))
            refresh()
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onStateChanged(currentState().copy(message = result.debugMessage))
        }
    }

    fun close() {
        billingClient.endConnection()
        isConnected = false
        isConnecting = false
    }

    private fun processPurchase(purchase: Purchase) {
        if (PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> onStateChanged(
                currentState().copy(loading = false, pending = true),
            )
            Purchase.PurchaseState.PURCHASED -> {
                preferences.edit().putBoolean(KEY_OWNED, true).apply()
                onStateChanged(currentState().copy(owned = true, pending = false, loading = false))
                if (!purchase.isAcknowledged) {
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build(),
                    ) { result ->
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            onStateChanged(currentState().copy(message = result.debugMessage))
                        }
                    }
                }
            }
        }
    }

    private fun currentState(): ProPurchaseState = ProPurchaseState(
        owned = preferences.getBoolean(KEY_OWNED, false),
        price = productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice,
    )

    companion object {
        const val PRODUCT_ID = "esim_checker_pro_lifetime"
        private const val PREFERENCES = "esim_checker_pro_purchase"
        private const val KEY_OWNED = "owned"
    }
}

data class ProPurchaseState(
    val owned: Boolean = false,
    val price: String? = null,
    val loading: Boolean = false,
    val pending: Boolean = false,
    val message: String? = null,
)
