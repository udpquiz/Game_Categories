package com.game.categories.adapter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.bumptech.glide.Glide
import com.game.categories.BillingClientHelper
import com.game.categories.R
import com.game.categories.api.BASEURL
import com.game.categories.databinding.VerticalRecyclerItemBinding
import com.game.categories.model.Datum
import com.game.categories.ui.AllCategories
import com.game.categories.ui.HomeActivity
import com.game.categories.utils.Constant
import com.game.categories.utils.Constant.toaster
import com.game.categories.utils.Pref
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class HorizontalAdapter(val context:Context,val data:List<Datum>):RecyclerView.Adapter<HorizontalAdapter.MyViewHolder>(),BillingClientHelper.BillingUpdatesListener{
    private var skuDetails: SkuDetails? = null
    private lateinit var billingClientHelper: BillingClientHelper
    lateinit var buy_inapp:Button
    var buyIdList:MutableList<Int> = mutableListOf()
    var dialog:AlertDialog? = null

    object idd{
        var id1:Int = 0
    }

    init {
        billingClientHelper = BillingClientHelper(context, this)
        billingClientHelper.startConnection {
            billingClientHelper.queryAvailableProducts(listOf("test1"))
        }

        val loadedBuyIdList = loadBuyIdList(context)
        if (loadedBuyIdList != null) {
            buyIdList = loadedBuyIdList
        } else {
            buyIdList = mutableListOf()
        }
    }
    class MyViewHolder(binding: VerticalRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root){
        val textView = binding.txtImg
        val img_view = binding.imgView
        val main_layout = binding.mainLayout
    }
    fun onLongClick(textView:TextView,img_view:ImageView,desc_txt:String,id:Int): Boolean {
        idd.id1 = id
        val view = LayoutInflater.from(context).inflate(R.layout.item_dialog, null)
        val dialogbuilder = AlertDialog.Builder(context)
        val txt:TextView = view.findViewById(R.id.txt_header)
        val txt_desc:TextView = view.findViewById(R.id.txt_desc)
        val img:ImageView = view.findViewById(R.id.img_item)
        val buy_qb:Button = view.findViewById(R.id.buy_qb)
        buy_inapp = view.findViewById(R.id.buy_inapp)

        if(loadBuyIdList(context) == null){
            buyIdList = mutableListOf()
        }
        else
        {
            buyIdList = loadBuyIdList(context)!!
        }
        val getIdList = loadBuyIdList(context)
        if (getIdList != null) {
            for(i in getIdList) {
                if (i == id) {
                    toaster(context,"Already Bought",1)
                    buy_qb.visibility = View.GONE
                    buy_inapp.visibility = View.GONE
                }
            }
        }
        buy_qb.setOnClickListener {
            if (Pref.getValue("qb") < 250){
                toaster(context,"Not Enough QB",1)
                Pref.addQb(1000)
                return@setOnClickListener
            }else{
                    showVideoAd(false,id,textView.text.toString())

            }
        }
        buy_inapp.setOnClickListener {
            showVideoAd(true,null,null)
        }
        txt.text = textView.text
        txt_desc.text = desc_txt
        img.setImageDrawable(img_view.drawable)
        dialogbuilder.setView(view)
        dialog = dialogbuilder.create()
        dialog?.show()
        return true
    }
    fun showVideoAd(isInApp:Boolean,id:Int?,string:String?){
        if (Constant.rewardedAd != null){
            Constant.rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback(){
                override fun onAdClicked() {
                    super.onAdClicked()
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    if(isInApp){
                        Log.e("Button", skuDetails.toString())
                        skuDetails?.let {
                            billingClientHelper.initiatePurchase((context as Activity)!!, it)
                        } ?: Log.e("Button", "SKU details not available")
                        Log.e("REWARD","REWARD GIVEN")
                    }
                    else
                    {
                        Pref.subQb(250)
                        buyIdList.add(id!!)
                        saveBuyIdList(buyIdList)
                        toaster(context, "Bought $string",1)
                        dialog?.dismiss()
                    }
                    Constant.rewardedAd = null
                    Constant.loadVideoAd(context)
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
            Constant.rewardedAd?.let { ad ->
                ad.show(context as Activity, OnUserEarnedRewardListener { rewardItem ->
                    // Handle the reward.
//                    Constant.loadVideoAd(context)
                })
            } ?: run {
            }
        }
        else{
            Log.e("Button", skuDetails.toString())
            skuDetails?.let {
                billingClientHelper.initiatePurchase((context as Activity)!!, it)
            } ?: Log.e("Button", "SKU details not available")
        }
    }
    override fun onSkuDetailsReceived(skuDetailsList: List<SkuDetails>) {
        if (skuDetailsList.isNotEmpty()) {
            skuDetails = skuDetailsList[0]
            buy_inapp.text = skuDetails?.price
        }
    }

    override fun onPurchasesUpdated(purchases: List<Purchase>) {
        for (purchase in purchases) {
            handlePurchase(purchase)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            buyIdList.add(idd.id1)
            saveBuyIdList(buyIdList)
            dialog?.dismiss()
            toaster(context, "Item Purchased Successfully", 1)
            if (!purchase.isAcknowledged) {
                billingClientHelper.acknowledgePurchase(purchase.purchaseToken) {
                    Log.e("Purchase", "Purchase acknowledged, consuming...")
                    consumePurchase(purchase.purchaseToken)
                    dialog?.dismiss()
                }
            } else {
                Log.e("Purchase", "Purchase acknowledged, consuming...")
                consumePurchase(purchase.purchaseToken)
            }
        }
    }
    private fun consumePurchase(purchaseToken: String) {
        billingClientHelper.consumePurchase(purchaseToken) {
            Log.e("ConsumePurchase", "Item consumed, purchaseToken: $purchaseToken")
            toaster(context, "Item Consumed", 1)
        }
    }
    fun saveBuyIdList(buyIdList: MutableList<Int>) {
        Pref.initialize(context)
        val gson = Gson()
        val jsonString = gson.toJson(buyIdList)
        Pref.setIdValue("BuyIdList",jsonString)
        Log.e("saveBuyIdList","List Saved in Pref")
    }
    fun loadBuyIdList(context: Context): MutableList<Int>? {
        val gson = Gson()
        val jsonString = Pref.getIdValue("BuyIdList")
        val type = object : TypeToken<MutableList<Int>>() {}.type
        return gson.fromJson(jsonString, type)
    }
    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): MyViewHolder {
        val binding = VerticalRecyclerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = data[position].category
        holder.main_layout.setOnLongClickListener {
            onLongClick(holder.textView,holder.img_view,data[position].description.toString(),data[position].id!!)
        }
        val imagefilename = data[position].buttonImage?.substringAfterLast('/')
        val imagePath = Environment.getExternalStorageDirectory().absolutePath + "/Android/data/com.game.categories/files/Download/$imagefilename"
        val imageUrl=if (HomeActivity.checkBox.isChecked){
            BASEURL+data[position].buttonImage
        }else{
            imagePath
        }
        Glide.with(holder.img_view.context)
            .load(imageUrl)
            .placeholder(R.drawable.download) // Placeholder image while loading
            .error(R.drawable.error_image) // Image to display on error
            .into(holder.img_view)

    }

    override fun getItemCount(): Int {
        return data.size
    }
}