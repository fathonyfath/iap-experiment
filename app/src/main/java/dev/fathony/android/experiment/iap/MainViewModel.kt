package dev.fathony.android.experiment.iap

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.asLiveData
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val playBillingIntegration: PlayBillingIntegration,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val availableProducts = listOf(
        Product("sample-subscription", Product.Type.Subscription),
    )

    private val _billingConnection =
        MutableLiveData<BillingConnectionStatus>(BillingConnectionStatus.NotStarted)
    val billingConnection: LiveData<BillingConnectionStatus> get() = _billingConnection

    private val _productDetails = MutableLiveData<List<ProductDetails>>(emptyList())
    val productDetails: LiveData<List<ProductDetails>> get() = _productDetails

    private val _currentBillingProduct = MutableLiveData<BillingProduct?>(null)
    val currentBillingProduct: LiveData<BillingProduct?> get() = _currentBillingProduct

    val availablePurchase = playBillingIntegration.availablePurchases.asLiveData()

    init {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) { playBillingIntegration.startConnection() }
                _billingConnection.value = BillingConnectionStatus.Connected

                val productDetailsResult = withContext(Dispatchers.IO) {
                    playBillingIntegration.queryProducts(availableProducts
                        .map { it.toPlayStoreProduct() }
                    )
                }
                _productDetails.value = productDetailsResult.productDetailsList.orEmpty()
            } catch (e: PlayBillingException) {
                _billingConnection.value = BillingConnectionStatus.Failed(e.resultCode)
            }
        }
    }

    fun prepareBillingFlow(billingProduct: BillingProduct) {
        _currentBillingProduct.value = billingProduct
    }

    fun cancelBillingFlowPreparation() {
        _currentBillingProduct.value = null
    }

    fun launchBillingFlow(activity: Activity, existingSubscription: ExistingSubscription?) {
        val billingProduct = currentBillingProduct.value
        checkNotNull(billingProduct)

        playBillingIntegration.launchBillingFlow(
            activity = activity,
            billingProduct = billingProduct,
            existingSubscription = existingSubscription,
        )
    }

    suspend fun refreshPurchases() {
        playBillingIntegration.refreshPurchases()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val application = checkNotNull(get(APPLICATION_KEY))
                val billingIntegration = PlayBillingIntegration(application)
                MainViewModel(billingIntegration, savedStateHandle)
            }
        }
    }
}
