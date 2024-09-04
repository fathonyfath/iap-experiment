package dev.fathony.android.experiment.iap

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ConnectionState
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlayBillingIntegration(context: Context) {

    private val billingClient: BillingClient
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            coroutineScope.launch { processPurchasesResult(purchases) }
        }
    }

    private val _availablePurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val availablePurchases: StateFlow<List<Purchase>> = _availablePurchases.asStateFlow()

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val acknowledgementLock = Mutex()

    private val billingClientTrigger =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    suspend fun startConnection() = suspendCoroutine { cont ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    billingClientTrigger.tryEmit(Unit)
                    cont.resume(Unit)
                } else {
                    cont.resumeWithException(PlayBillingException(billingResult.responseCode))
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    suspend fun queryProducts(products: List<QueryProductDetailsParams.Product>): ProductDetailsResult {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        return billingClient.queryProductDetails(params)
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        productOffer: String
    ) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(productOffer)
                .build()
        )

        BillingFlowParams.SubscriptionUpdateParams.newBuilder()

        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)

        billingClient.launchBillingFlow(activity, billingFlowParamsBuilder.build())
    }

    fun launchBillingFlow(
        activity: Activity,
        billingProduct: BillingProduct,
        existingSubscription: ExistingSubscription?,
    ) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(billingProduct.productDetails)
                .setOfferToken(billingProduct.offerToken)
                .build()
        )

        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)

        if (existingSubscription != null) {
            billingFlowParamsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(existingSubscription.purchaseToken)
                    .setSubscriptionReplacementMode(
                        existingSubscription.replacementMode.toPlayBillingReplacementMode()
                    )
                    .build()
            )
        }

        billingClient.launchBillingFlow(activity, billingFlowParamsBuilder.build())
    }

    suspend fun refreshPurchases() {
        val purchasesResult = obtainPurchasesResult()
        if (purchasesResult.billingResult.responseCode == BillingResponseCode.OK) {
            processPurchasesResult(purchasesResult.purchasesList)
        }
    }


    private suspend fun processPurchasesResult(purchasesList: List<Purchase>) {
        acknowledgementLock.withLock {
            val needToRefresh = purchasesList
                .filter { !it.isAcknowledged }
                .map { purchase ->
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                }
                .map { params ->
                    billingClient.acknowledgePurchase(params)
                }
                .any { it.responseCode == BillingResponseCode.OK }

            val purchases = if (needToRefresh) {
                obtainPurchasesResult().purchasesList
            } else {
                purchasesList
            }

            _availablePurchases.value = purchases
        }
    }

    private suspend fun obtainPurchasesResult(): PurchasesResult {
        if (billingClient.connectionState != ConnectionState.CONNECTED) {
            // Conditions to locks until billing client connection is established
            billingClientTrigger.take(1).collect()
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()

        val purchasesResult = billingClient.queryPurchasesAsync(params)
        return purchasesResult
    }
}
