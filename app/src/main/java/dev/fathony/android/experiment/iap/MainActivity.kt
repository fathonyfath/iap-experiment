package dev.fathony.android.experiment.iap

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dev.fathony.android.experiment.iap.databinding.ActivityMainBinding
import dev.fathony.android.experiment.iap.databinding.DialogConfirmationBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private val adapter = PlayBillingProductsAdapter()
    private val purchasesAdapter = PlayBillingPurchasesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.recycler.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomSheet) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.bottomSheet.doOnLayout {
                val contentHeight =
                    binding.bottomSheetHandle.measuredHeight + binding.billingStatus.measuredHeight

                binding.recycler.updatePadding(bottom = contentHeight + systemBars.bottom)
                binding.purchasesRecycler.updatePadding(bottom = systemBars.bottom)
                BottomSheetBehavior.from(view).also { behavior ->
                    behavior.isFitToContents = false
                    behavior.expandedOffset = systemBars.top
                    behavior.peekHeight = contentHeight
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            insets
        }

        adapter.setOnItemClickListener { product ->
            viewModel.prepareBillingFlow(product)
        }

        binding.recycler.also { recycler ->
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            recycler.adapter = adapter
        }

        binding.purchasesRecycler.also { recycler ->
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            recycler.adapter = purchasesAdapter
        }

        binding.openSubscriptionManagement.setOnClickListener {
            val subscriptionUriBuilder =
                Uri.parse("https://play.google.com/store/account/subscriptions").buildUpon()

            val availablePurchases = viewModel.availablePurchase.value.orEmpty()

            if (availablePurchases.size == 1) {
                val purchase = availablePurchases.first()

                subscriptionUriBuilder
                    .appendQueryParameter("sku", purchase.products.first())
                    .appendQueryParameter("package", purchase.packageName)
            }

            val intent = Intent(Intent.ACTION_VIEW, subscriptionUriBuilder.build())
            startActivity(intent)
        }

        viewModel.billingConnection.observe(this) { connection ->
            val status = when (connection) {
                BillingConnectionStatus.Connected -> "Connected"
                is BillingConnectionStatus.Failed -> "Failed. Reason: ${connection.responseCode.billingResultCodeToString()}"
                BillingConnectionStatus.NotStarted -> "Not Started"
            }

            @SuppressLint("SetTextI18n")
            binding.billingStatus.text = "Billing status: $status"
        }

        viewModel.productDetails.observe(this) { productDetails ->
            if (productDetails.isNotEmpty()) {
                adapter.setProductDetails(productDetails)
            }
        }

        viewModel.currentBillingProduct.observe(this) { currentBillingProduct ->
            if (currentBillingProduct != null) {
                lifecycleScope.launch { viewModel.refreshPurchases() }
                launchConfirmationDialog(currentBillingProduct)
            }
        }

        viewModel.availablePurchase.observe(this) { availablePurchases ->
            purchasesAdapter.availablePurchases = availablePurchases
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshPurchases()
            }
        }
    }

    private fun launchConfirmationDialog(billingProduct: BillingProduct) {
        val currentBillingProduct = checkNotNull(viewModel.currentBillingProduct.value)

        currentBillingProduct.productDetails.productId

        val dialogBinding = DialogConfirmationBinding.inflate(layoutInflater)
        when (billingProduct) {
            is BillingProduct.Base -> {
                dialogBinding.basePlanIdValue.text = billingProduct.id
                dialogBinding.offerIdValue.text = "-"
            }

            is BillingProduct.Offer -> {
                dialogBinding.basePlanIdValue.text = billingProduct.baseId
                dialogBinding.offerIdValue.text = billingProduct.id
            }
        }
        dialogBinding.baseProductIdValue.text = billingProduct.productDetails.productId

        val availablePurchases = viewModel.availablePurchase.value.orEmpty()
        val isSwitchingBasePlan = availablePurchases
            .flatMap { it.products }
            .contains(currentBillingProduct.productDetails.productId)

        dialogBinding.replaceSubscription.isEnabled = !isSwitchingBasePlan
        dialogBinding.replaceSubscription.isChecked = false
        dialogBinding.availableSubscriptions.isEnabled = dialogBinding.replaceSubscription.isChecked
        dialogBinding.replacementMode.isEnabled = dialogBinding.replaceSubscription.isChecked
        dialogBinding.changeBasePlanNotification.isVisible = isSwitchingBasePlan

        val availablePurchasesDropdown = availablePurchases
            .mapIndexed { i, purchase -> "${i + 1} - ${purchase.orderId ?: "orderId not available"}" }

        val availableSubscriptionsDropdown =
            dialogBinding.availableSubscriptions.editText as? MaterialAutoCompleteTextView
        availableSubscriptionsDropdown?.setSimpleItems(availablePurchasesDropdown.toTypedArray())

        var selectedAvailablePurchasesPosition: Int? = null

        val availableReplacementMode = ExistingSubscription.ReplacementMode.entries.map { it.name }

        val replacementModeDropdown =
            dialogBinding.replacementMode.editText as? MaterialAutoCompleteTextView
        replacementModeDropdown?.setSimpleItems(availableReplacementMode.toTypedArray())

        var selectedReplacementModePosition: Int? = null

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Launch Billing Flow")
            .setView(dialogBinding.root)
            .setPositiveButton("Launch") { _, _ ->
                val replaceSubscriptionChecked = dialogBinding.replaceSubscription.isChecked
                val existingSubscription: ExistingSubscription? = if (replaceSubscriptionChecked) {
                    val purchasesPosition = checkNotNull(selectedAvailablePurchasesPosition)
                    val replacementModePosition = checkNotNull(selectedReplacementModePosition)

                    val purchaseToReplace = availablePurchases[purchasesPosition]
                    val replacementMode =
                        ExistingSubscription.ReplacementMode.entries[replacementModePosition]

                    ExistingSubscription(purchaseToReplace.purchaseToken, replacementMode)
                } else {
                    null
                }

                viewModel.launchBillingFlow(this, existingSubscription)
            }
            .setNegativeButton("Close", null)
            .setOnDismissListener { viewModel.cancelBillingFlowPreparation() }
            .show()

        availableSubscriptionsDropdown?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedAvailablePurchasesPosition = position

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    selectedReplacementModePosition != null
            }

        replacementModeDropdown?.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectedReplacementModePosition = position

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    selectedAvailablePurchasesPosition != null
            }

        dialogBinding.replaceSubscription.setOnCheckedChangeListener { _, checked ->
            dialogBinding.availableSubscriptions.isEnabled = checked
            dialogBinding.replacementMode.isEnabled = checked

            if (checked) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    selectedReplacementModePosition != null && selectedAvailablePurchasesPosition != null
            } else {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }

        // Only enable positive button if replace subscription is not checked, and if the replacement selection is valid
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            !dialogBinding.replaceSubscription.isChecked ||
                    (selectedAvailablePurchasesPosition != null
                            && selectedReplacementModePosition != null)
    }
}
