package com.game.categories

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

class BillingClientHelper(private val context: Context, private val listener: BillingUpdatesListener) {

    private lateinit var billingClient: BillingClient

    interface BillingUpdatesListener {
        fun onSkuDetailsReceived(skuDetailsList: List<SkuDetails>)
        fun onPurchasesUpdated(purchases: List<Purchase>)
    }

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    listener.onPurchasesUpdated(purchases)
                } else {
                    Log.e("BillingClient", "Error in purchase listener: ${billingResult.debugMessage}")
                }
            }
            .enablePendingPurchases()
            .build()
    }

    fun startConnection(onConnected: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                } else {
                    Log.e("BillingClient", "Error in setup finished: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("BillingClient", "Billing service disconnected")
            }
        })
    }

    fun queryAvailableProducts(skuList: List<String>) {
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                listener.onSkuDetailsReceived(skuDetailsList)
            } else {
                Log.e("BillingClient", "Error in querySkuDetailsAsync: ${billingResult.debugMessage}")
            }
        }
    }

    fun initiatePurchase(activity: Activity, skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e("PurchaseFlow", "Error in launchBillingFlow: ${billingResult.debugMessage}")
        }
    }

    fun acknowledgePurchase(purchaseToken: String, onAcknowledged: () -> Unit) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onAcknowledged()
            } else {
                Log.e("PurchaseFlow", "Error in acknowledgePurchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun consumePurchase(purchaseToken: String, onConsumed: () -> Unit) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onConsumed()
            } else {
                Log.e("ConsumePurchase", "Error consuming purchase: ${billingResult.debugMessage}")
            }
        }
    }
}