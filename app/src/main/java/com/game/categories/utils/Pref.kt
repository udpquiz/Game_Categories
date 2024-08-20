package com.game.categories.utils

import android.content.Context
import android.content.SharedPreferences

object Pref {
    private var sharedPreferences: SharedPreferences? = null
    fun initialize(context: Context){
        sharedPreferences = context.getSharedPreferences(Constant.pref, Context.MODE_PRIVATE)
    }
    fun setValue(key: String, value: Int) {
        sharedPreferences?.edit()?.putInt(key,value)?.apply()
    }
    fun getValue(key: String):Int{
        return sharedPreferences?.getInt(key,0) ?:0
    }
    fun setIdValue(key: String, value: String) {
        sharedPreferences?.edit()?.putString(key,value)?.apply()
    }
    fun getIdValue(key: String):String{
       return sharedPreferences?.getString(key,"") ?:""
    }

    fun addQb(amount:Int){
        val qb = getValue("qb")
        setValue("qb",qb+amount)
    }
    fun subQb(amount:Int){
        val qb = getValue("qb")
        setValue("qb",qb-amount)
    }
}