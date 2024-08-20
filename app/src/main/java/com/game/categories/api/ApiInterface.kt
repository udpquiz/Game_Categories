package com.game.categories.api

import com.game.categories.model.MainData
import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {
    @GET("storage/category_data.json")
    fun getFetchAllCategory(): Call<MainData>
}