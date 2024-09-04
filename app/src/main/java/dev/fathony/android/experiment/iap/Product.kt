package dev.fathony.android.experiment.iap

import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.QueryProductDetailsParams

data class Product(val id: String, val type: Type) {

    fun toPlayStoreProduct(): QueryProductDetailsParams.Product {
        val type = when (type) {
            Type.InAppProduct -> ProductType.INAPP
            Type.Subscription -> ProductType.SUBS
        }

        return QueryProductDetailsParams.Product.newBuilder()
            .setProductId(id)
            .setProductType(type)
            .build()
    }

    enum class Type {
        InAppProduct,
        Subscription,
    }
}
