package dev.fathony.android.experiment.iap

import com.android.billingclient.api.BillingClient.BillingResponseCode

class PlayBillingException(@BillingResponseCode val resultCode: Int) : Exception()
