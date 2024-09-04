package dev.fathony.android.experiment.iap

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import dev.fathony.android.experiment.iap.databinding.ItemOfferBinding
import dev.fathony.android.experiment.iap.databinding.ItemProductBinding

class PlayBillingProductsAdapter :
    RecyclerView.Adapter<PlayBillingProductsAdapter.BillingViewHolder>() {

    fun interface OnItemClickListener {
        fun onClick(model: BillingProduct)
    }

    private var renderItem: List<BillingProduct> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var listener: OnItemClickListener? = null

    fun setProductDetails(productDetails: List<ProductDetails>) {
        val offers = productDetails.map { it to it.subscriptionOfferDetails.orEmpty() }
            .flatMap { (product, plans) ->
                plans.map { plan ->
                    val offerId = plan.offerId
                    if (offerId == null) {
                        BillingProduct.Base(plan.basePlanId, plan.offerToken, product)
                    } else {
                        BillingProduct.Offer(offerId, plan.basePlanId, plan.offerToken, product)
                    }
                }.reversed()
            }
        renderItem = offers
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillingViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        return when (viewType) {
            BASE_VIEW_TYPE -> ProductItem(
                ItemProductBinding.inflate(inflater, parent, false)
            )

            OFFER_VIEW_TYPE -> OfferItem(
                ItemOfferBinding.inflate(inflater, parent, false)
            )

            else -> throw IllegalStateException()
        }
    }

    override fun getItemCount(): Int = this.renderItem.size

    override fun onBindViewHolder(holder: BillingViewHolder, position: Int) {
        when (holder) {
            is OfferItem -> holder.bind(this.renderItem[position], listener)
            is ProductItem -> holder.bind(this.renderItem[position], listener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (this.renderItem[position]) {
            is BillingProduct.Base -> BASE_VIEW_TYPE
            is BillingProduct.Offer -> OFFER_VIEW_TYPE
        }
    }

    sealed class BillingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private class ProductItem(val binding: ItemProductBinding) :
        BillingViewHolder(binding.root) {

        fun bind(product: BillingProduct, listener: OnItemClickListener?) {
            require(product is BillingProduct.Base)
            binding.id.text = product.id

            binding.root.setOnClickListener {
                listener?.onClick(product)
            }
        }
    }

    private class OfferItem(val binding: ItemOfferBinding) : BillingViewHolder(binding.root) {
        fun bind(product: BillingProduct, listener: OnItemClickListener?) {
            require(product is BillingProduct.Offer)
            binding.id.text = product.id
            binding.baseId.text = product.baseId

            binding.root.setOnClickListener {
                listener?.onClick(product)
            }
        }
    }

    companion object {
        private const val BASE_VIEW_TYPE = 1
        private const val OFFER_VIEW_TYPE = 2
    }
}
