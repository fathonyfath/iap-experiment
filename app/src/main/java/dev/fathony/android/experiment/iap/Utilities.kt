package dev.fathony.android.experiment.iap

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase.PurchaseState

@Suppress("DEPRECATION")
@BillingResponseCode
fun Int.billingResultCodeToString(): String = when (this) {
    BillingResponseCode.SERVICE_TIMEOUT -> "SERVICE_TIMEOUT"
    BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
    BillingResponseCode.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
    BillingResponseCode.OK -> "OK"
    BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
    BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
    BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
    BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
    BillingResponseCode.ERROR -> "ERROR"
    BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
    BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
    BillingResponseCode.NETWORK_ERROR -> "NETWORK_ERROR"
    else -> throw IllegalStateException()
}

@PurchaseState
fun Int.purchaseStateToString(): String = when (this) {
    PurchaseState.UNSPECIFIED_STATE -> "UNSPECIFIED_STATE"
    PurchaseState.PURCHASED -> "PURCHASED"
    PurchaseState.PENDING -> "PENDING"
    else -> throw IllegalStateException()
}
