package dev.fathony.android.experiment.iap

import com.android.billingclient.api.BillingClient.BillingResponseCode

sealed interface BillingConnectionStatus {
    data object Connected : BillingConnectionStatus
    data class Failed(@BillingResponseCode val responseCode: Int) : BillingConnectionStatus
    data object NotStarted : BillingConnectionStatus
}
