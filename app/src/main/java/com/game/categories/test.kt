package com.game.categories

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*

class test : AppCompatActivity() {
    private lateinit var billingClient: BillingClient
    var skudetails: SkuDetails? = null
    lateinit var btnBuy: Button
    var purchaseToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

//        billingClient = BillingClient.newBuilder(this)
//            .setListener { billingResult, purchases ->
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
//                    for (purchase in purchases) {
//                        handlePurchase(purchase)
//                    }
//                } else {
//                    Log.e("BillingClient", "Error in purchase listener: ${billingResult.debugMessage}")
//                }
//            }
//            .enablePendingPurchases()
//            .build()

        btnBuy = findViewById(R.id.buy_button)
        btnBuy.setOnClickListener {
            Log.e("Button", "Button clicked")
            Log.e("11",skudetails.toString())
            skudetails?.let {
                initiatePurchase(it)
            } ?: run {
                Log.e("Button", "SKU details not available")
            }
        }
//        connectToGooglePlay()
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.e("BillingClient", "Billing setup finished successfully")
                    queryAvailableProducts()
                } else {
                    Log.e("BillingClient", "Error in setup finished: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("BillingClient", "Billing service disconnected")
            }
        })
    }

    private fun queryAvailableProducts() {
        val skuList = listOf("test1")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (skuDetails in skuDetailsList) {
                    Log.e("BillingClient", "SKU Details received: ${skuDetails.sku}, ${skuDetails.title}")
                    showProduct(skuDetails)
                }
            } else {
                Log.e("BillingClient", "Error in querySkuDetailsAsync: ${billingResult.debugMessage}")
            }
        }
    }

    private fun showProduct(skuDetails: SkuDetails) {
        skudetails = skuDetails
        btnBuy.apply {
            text = skuDetails.price
        }
    }

    private fun initiatePurchase(skuDetails: SkuDetails) {
        Log.e("PurchaseFlow", "Initiate purchase")
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val billingResult = billingClient.launchBillingFlow(this, flowParams)
        Log.e("PurchaseFlow", "Billing flow response code: ${billingResult.responseCode}")
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e("PurchaseFlow", "Error in launchBillingFlow: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Log.e("PurchaseFlow", "Purchase successful")
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e("PurchaseFlow", "Purchase acknowledged")
                        // Consume the purchase immediately after acknowledgment
                        consumePurchase(purchase.purchaseToken)
                    } else {
                        Log.e("PurchaseFlow", "Error in acknowledgePurchase: ${billingResult.debugMessage}")
                    }
                }
            } else {
                // If the purchase is already acknowledged, consume it immediately
                consumePurchase(purchase.purchaseToken)
            }
        } else {
            Log.e("PurchaseFlow", "Purchase not completed: ${purchase.purchaseState}")
        }
    }


    private fun consumePurchase(purchaseToken: String) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { billingResult, token ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.e("ConsumePurchase", "Item consumed")
                // Handle the success of the consume operation.
                this.purchaseToken = null  // Clear the token after consumption
            } else {
                Log.e("ConsumePurchase", "Error consuming purchase: ${billingResult.debugMessage}")
            }
        }
    }
}
