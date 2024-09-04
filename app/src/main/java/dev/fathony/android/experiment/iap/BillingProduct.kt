package dev.fathony.android.experiment.iap

import com.android.billingclient.api.ProductDetails

sealed class BillingProduct(val offerToken: String, val productDetails: ProductDetails) {
    class Base(
        val id: String,
        offerToken: String,
        productDetails: ProductDetails
    ) : BillingProduct(offerToken, productDetails)

    class Offer(
        val id: String,
        val baseId: String,
        offerToken: String,
        productDetails: ProductDetails
    ) : BillingProduct(offerToken, productDetails)
}
