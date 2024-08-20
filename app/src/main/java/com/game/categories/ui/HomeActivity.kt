package com.game.categories.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.internal.ApiFeature
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.game.categories.BillingClientHelper
import com.game.categories.R
import com.game.categories.databinding.ActivityMainBinding
import com.game.categories.test
import com.game.categories.utils.Constant
import com.game.categories.utils.Constant.toaster
import com.game.categories.utils.Constant.topics
import com.game.categories.utils.Pref
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity(), BillingClientHelper.BillingUpdatesListener {
    private lateinit var billingClientHelper: BillingClientHelper
    private lateinit var binding: ActivityMainBinding
    private var skuDetails: SkuDetails? = null
    lateinit var btnBuy500qb:Button
    private lateinit var qb:TextView
    private var mInterstitialAd: InterstitialAd? = null

    object checkBox {
        var isChecked: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Firebase.initialize(this)
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.e("FCM Token", "Token: $token")
            }
        }
        handleIntent(intent)
        Constant.loadBannerAd(this,binding.adView)
        Constant.loadInterAd(this)
        Constant.loadVideoAd(this)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,topics)
        binding.topicSpinner.adapter = adapter
        binding.topicSpinner.setSelection(topics.indexOf(Pref.getIdValue("topic")))
        binding.topicSpinner.onItemSelectedListener=object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                for(i in topics){
                    unsubscribeTopic(i)
                }
                subscribeTopic(topics[position])
                Pref.setIdValue("topic",topics[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        Pref.initialize(this)
        billingClientHelper = BillingClientHelper(this, this)
        val view = LayoutInflater.from(this).inflate(R.layout.buy_qb, null)
        qb = view.findViewById<TextView>(R.id.txt_qbamt)
        updateQbText()
        binding.txtMainqb.text = Pref.getValue("qb").toString()
        binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if(binding.checkBox.isChecked){
                checkBox.isChecked = true
            }
            else
            {
                checkBox.isChecked = false
            }
        }

        binding.btnAllcatg.setOnClickListener {
            showInterAd(this, AllCategories::class.java)
//            startActivity(Intent(this, AllCategories::class.java))
        }
        binding.btnShop.setOnClickListener {
            showInterAd(this, Shop::class.java)
//            startActivity(Intent(this, Shop::class.java))
        }
        binding.qbMain.setOnClickListener {
            showPurchaseDialog()
        }
        billingClientHelper.startConnection {
            billingClientHelper.queryAvailableProducts(listOf("test1"))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.extras?.let {
            val topic = it.getString("topic")
            Log.d("HomeActivity", "Received topic: $topic")
            // Handle the topic as needed
            val intent = if (topic == "IND") {
                Intent(this, Shop::class.java)
            } else if (topic == "UK") {
                Intent(this, AllCategories::class.java)
            } else {
                Intent(this, HomeActivity::class.java)
            }
            startActivity(intent)
        }
    }

    fun subscribeTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnSuccessListener {
            toaster(this, "Subscribed $topic", 1)
        }.addOnFailureListener {
            toaster(this, "Failed to Subscribe $topic", 1)
        }
    }
    fun unsubscribeTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnSuccessListener {
            toaster(this, "UnSubscribed $topic", 1)
        }.addOnFailureListener {
            toaster(this, "Failed to UnSubscribe $topic", 1)
        }
    }
    fun <T> showInterAd(context: Context, targetActivity: Class<T>){
        if (Constant.mInterstitialAd != null) {
            Constant.mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback(){
                override fun onAdClicked() {
                    super.onAdClicked()
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    val intent = Intent(context, targetActivity)
                    context.startActivity(intent)
                    Constant.loadInterAd(this@HomeActivity)
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    super.onAdFailedToShowFullScreenContent(p0)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                }

            }
            Constant.mInterstitialAd?.show(this)
        } else {
            val intent = Intent(context, targetActivity)
            context.startActivity(intent)
            Constant.loadInterAd(this@HomeActivity)
            Log.d("Inter AD", "The interstitial ad wasn't ready yet.")
        }
    }

    private fun showPurchaseDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.buy_qb, null)
        val dialog = AlertDialog.Builder(this)
        val btnConsume = view.findViewById<TextView>(R.id.btn_consume)
        btnConsume.setOnClickListener {
            Pref.setValue("qb", 0)
            updateQbText()
            Toast.makeText(this, "All QB Cleared", Toast.LENGTH_SHORT).show()
        }
        qb = view.findViewById<TextView>(R.id.txt_qbamt)
        btnBuy500qb = view.findViewById<Button>(R.id.btn_buy500qb)
        btnBuy500qb.setOnClickListener {
            Log.e("Button", skuDetails.toString())
            skuDetails?.let {
                billingClientHelper.initiatePurchase(this, it)
            } ?: Log.e("Button", "SKU details not available")
        }
        qb.text = Pref.getValue("qb").toString()
        dialog.setView(view)
        dialog.setOnDismissListener {
            updateQbText()
        }
        dialog.show()
    }

    private fun updateQbText() {
        binding.txtMainqb.text = Pref.getValue("qb").toString()
        qb.text = Pref.getValue("qb").toString()

    }

    override fun onSkuDetailsReceived(skuDetailsList: List<SkuDetails>) {
        if (skuDetailsList.isNotEmpty()) {
            skuDetails = skuDetailsList[0]
            btnBuy500qb.text = skuDetails?.price
        }
    }

    override fun onPurchasesUpdated(purchases: List<Purchase>) {
        for (purchase in purchases) {
            handlePurchase(purchase)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Pref.addQb(500)
            updateQbText()
            if (!purchase.isAcknowledged) {
                billingClientHelper.acknowledgePurchase(purchase.purchaseToken) {
                    Log.e("Purchase", "Purchase acknowledged, consuming...")
                    consumePurchase(purchase.purchaseToken)
                }
            } else {
                Log.e("Purchase", "Purchase already acknowledged, consuming directly...")
                consumePurchase(purchase.purchaseToken)
            }
        }
    }
    private fun consumePurchase(purchaseToken: String) {
        billingClientHelper.consumePurchase(purchaseToken) {
            Log.e("ConsumePurchase", "Item consumed, purchaseToken: $purchaseToken")
            toaster(applicationContext, "Item Consumed", 1)
        }
    }

    override fun onResume() {
        super.onResume()
        updateQbText()
    }
}
