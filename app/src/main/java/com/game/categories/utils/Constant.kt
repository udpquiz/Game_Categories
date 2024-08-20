package com.game.categories.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.game.categories.ui.Shop
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object Constant {
    var mInterstitialAd: InterstitialAd? = null
    var rewardedAd: RewardedAd? = null
    const val pref = "MyPrefs"
    val topics = arrayOf("IND","UK","USA")
    fun toaster(context: Context, message: String, seconds: Long) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast.show()

        Handler(Looper.getMainLooper()).postDelayed({
            toast.cancel()
        }, seconds*1000)
    }
    fun loadBannerAd(context: Context,adview : AdView) {
        MobileAds.initialize(context)
        val adView:AdView = adview
        val adRequest:AdRequest= AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
    fun loadInterAd(context: Context){
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("Inter AD", adError.toString())
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("Inter AD", "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })
    }
    fun loadVideoAd(context: Context){
        var adRequest = AdRequest.Builder().build()
        RewardedAd.load(context,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }


}