package dev.fathony.android.experiment.iap

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.Purchase
import dev.fathony.android.experiment.iap.databinding.ItemPurchaseBinding
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PlayBillingPurchasesAdapter :
    RecyclerView.Adapter<PlayBillingPurchasesAdapter.PurchaseViewHolder>() {

    var availablePurchases: List<Purchase> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val context = parent.context
        val layoutInflater = LayoutInflater.from(context)
        val binding = ItemPurchaseBinding.inflate(layoutInflater, parent, false)
        return PurchaseViewHolder(binding)
    }

    override fun getItemCount(): Int = availablePurchases.size

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        holder.bind(availablePurchases[position])
    }

    class PurchaseViewHolder(private val binding: ItemPurchaseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(purchase: Purchase) {
            binding.orderIdValue.text = purchase.orderId
            binding.productsValue.text = purchase.products.joinToString()

            val purchaseTimeInstant = Instant.fromEpochMilliseconds(purchase.purchaseTime)
            val localPurchaseTime =
                purchaseTimeInstant.toLocalDateTime(TimeZone.currentSystemDefault())
            binding.purchaseTimeValue.text = localPurchaseTime.toString()

            binding.purchaseStateValue.text = purchase.purchaseState.purchaseStateToString()
            binding.isAcknowledgedValue.text = purchase.isAcknowledged.toString()
            binding.isAutoRenewingValue.text = purchase.isAutoRenewing.toString()
            binding.packageNameValue.text = purchase.packageName
        }
    }
}
