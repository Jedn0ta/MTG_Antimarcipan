package com.example.antimarcipan.support

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * „Kafe pro autora“ přes Google Play Billing — jednorázový produkt [PRODUCT_ID].
 * Po zaplacení se odemkne odznak Mecenáš (jen kosmetika).
 */
object SupportStore {
    const val AVAILABLE = true
    const val PRODUCT_ID = "kafe_pro_autora"

    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price
    val message = MutableStateFlow<String?>(null)

    private var client: BillingClient? = null
    private var details: ProductDetails? = null
    private lateinit var app: Context

    fun connect(ctx: Context) {
        app = ctx.applicationContext
        Supporter.load(app)
        if (client != null) return
        val c = BillingClient.newBuilder(app)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases?.forEach(::handle)
                else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED)
                    message.value = "Nákup se nepovedl (${result.responseCode})."
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode != BillingClient.BillingResponseCode.OK) return
                restore()
                loadDetails()
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun loadDetails() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        ).build()
        client?.queryProductDetailsAsync(params) { _, result ->
            details = result.productDetailsList.firstOrNull()
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    /** Obnoví odznak po přeinstalaci / na jiném zařízení se stejným účtem. */
    private fun restore() {
        client?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { _, list -> list.forEach(::handle) }
    }

    private fun handle(p: Purchase) {
        if (PRODUCT_ID !in p.products) return
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (p.purchaseState == Purchase.PurchaseState.PENDING) message.value = "Platba čeká na dokončení."
            return
        }
        Supporter.grant(app)
        message.value = "Díky za kafe! ☕ Odznak Mecenáš je tvůj."
        if (!p.isAcknowledged) {
            client?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            ) {}
        }
    }

    fun buy(activity: Activity) {
        val d = details
        if (d == null) { message.value = "Obchod zatím není dostupný, zkus to za chvíli."; loadDetails(); return }
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())
        ).build()
        client?.launchBillingFlow(activity, flow)
    }
}
