package dev.fathony.android.experiment.iap

import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams

data class ExistingSubscription(
    val purchaseToken: String,
    val replacementMode: ReplacementMode,
) {
    enum class ReplacementMode {
        UNKNOWN_REPLACEMENT_MODE,
        WITH_TIME_PRORATION,
        CHARGE_PRORATED_PRICE,
        WITHOUT_PRORATION,
        CHARGE_FULL_PRICE,
        DEFERRED,
        ;

        @SubscriptionUpdateParams.ReplacementMode
        fun toPlayBillingReplacementMode(): Int {
            return when (this) {
                UNKNOWN_REPLACEMENT_MODE -> SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE
                WITH_TIME_PRORATION -> SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                CHARGE_PRORATED_PRICE -> SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
                WITHOUT_PRORATION -> SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION
                CHARGE_FULL_PRICE -> SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
                DEFERRED -> SubscriptionUpdateParams.ReplacementMode.DEFERRED
            }
        }
    }
}
