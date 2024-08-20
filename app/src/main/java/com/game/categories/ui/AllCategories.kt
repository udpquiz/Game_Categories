package com.game.categories.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.game.categories.R
import com.game.categories.adapter.VerticalAdapter
import com.game.categories.api.ApiClient
import com.game.categories.api.ApiInterface
import com.game.categories.api.BASEURL
import com.game.categories.model.Datum
import com.game.categories.model.MainData
import com.game.categories.model.Tag
import com.game.categories.utils.Constant
import com.game.categories.utils.Constant.toaster
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AllCategories : AppCompatActivity() {
    private lateinit var adapter: VerticalAdapter
    lateinit var tag: MutableLiveData<List<Tag>>
    lateinit var data: List<Datum>
    lateinit var recyclerView: RecyclerView
    private val client = OkHttpClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        Log.e("11111", getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString())
        initView()
        Log.e("0000","In ALlCat")
        val tslist = loadTimeStampList(this)
        if (tslist != null) {
            for (ts in tslist) {
                Log.e("ID", ts.id.toString())
                Log.e("TIMESTAMP", ts.buttonImageTimestamp.toString())
            }
        }
    }

    private fun initView() {
        val folder = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString())
        if (folder.exists() && folder.isDirectory) {
            Log.e("Images ","Folder Exists")
            val files = folder.listFiles()
            if (files == null || files.isEmpty()) {
                getFetchAllCategory()
                Log.e("Images ","Downloading Images")
                toaster(this,"Downloading Images",2)
            }
            else
            {
                if(HomeActivity.checkBox.isChecked){
                    getFetchAllCategory()
                    Log.e("API","Downloading Images")
                }else {
                    getFetchAllCategory1()
                    Log.e("API", "Getting Images from Local")
                }
            }
        }
        else
        {
            Log.e("Images ","Folder Not Exists")
        }
        recyclerView= findViewById(R.id.horizontal_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)

    }
    private fun getFetchAllCategory(){
        val apiInterface: ApiInterface = ApiClient.getApiClient().create(ApiInterface::class.java)
        apiInterface.getFetchAllCategory().enqueue(object : Callback<MainData> {
            override fun onResponse(call: Call<MainData>, response: Response<MainData>) {
                if (response.isSuccessful){
                    if(HomeActivity.checkBox.isChecked){
                    adapter = VerticalAdapter(this@AllCategories,response.body()?.data?.tags!!,response.body()?.data?.data!! , false)
                        recyclerView.adapter = adapter
                        toaster(applicationContext,"Getting From API",2)
                    }
                    else{
                        adapter = VerticalAdapter(this@AllCategories,response.body()?.data?.tags!!,response.body()?.data?.data!! , false)
                        data=response.body()?.data?.data!!
                        val timeStampList:MutableList<Datum> = mutableListOf()

                        for(data in data){
                            val buttonImage = data.buttonImage ?: ""
                            // Create a new Datum object
                            val datum = Datum()
                            datum.id = data.id
                            datum.buttonImageTimestamp = data.buttonImageTimestamp
                            timeStampList.add(datum)
                            if (buttonImage.length > 19) {
                                val fname = buttonImage.substring(19)
                                downloadImage(BASEURL + buttonImage, fname)
                            } else {
                                Log.e("Error", "buttonImage${data.id} string is too short: $buttonImage")
                            }
                            toaster(applicationContext,"Please Wait Images Downloading",2)

                        }
                        toaster(applicationContext,"Downloading Complete",2)
                        saveTimeStampList(timeStampList)
                        recyclerView.adapter = adapter
                    }

                }
                else{
                    Log.e("Failed Response", "Failed to fetch posts: ${response.code()}")
                    toaster(applicationContext,"Failed to fetch posts from API",2)
                    getFetchAllCategory()
                }
            }

            override fun onFailure(call: Call<MainData>, t: Throwable) {
                Log.e("Failure msg", "Failed to fetch posts",t)
                toaster(applicationContext,"Failed to fetch posts from API",2)
                getFetchAllCategory()
            }

        })

    }
    fun saveTimeStampList(timeStampList: MutableList<Datum>) {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences("TimestampList",Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val gson = Gson()
        val jsonString = gson.toJson(timeStampList)
        editor.putString("TimeStampList", jsonString)
        editor.apply()
    }
    fun loadTimeStampList(context: Context): MutableList<Datum>? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("TimestampList", Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonString = sharedPreferences.getString("TimeStampList", null)
        val type = object : TypeToken<MutableList<Datum>>() {}.type
        return gson.fromJson(jsonString, type)
    }
    private fun getFetchAllCategory1(){
        val apiInterface: ApiInterface = ApiClient.getApiClient().create(ApiInterface::class.java)
        apiInterface.getFetchAllCategory().enqueue(object : Callback<MainData> {
            override fun onResponse(call: Call<MainData>, response: Response<MainData>) {
                if (response.isSuccessful){
                    val tslist = loadTimeStampList(this@AllCategories)
                    if (tslist != null) {
                        for (ts in tslist) {
                            for (data in response.body()?.data?.data!!){
                                if (data.buttonImageTimestamp != ts.buttonImageTimestamp) {
//                                    If any Timestamp is changed then all images will download again
                                    getFetchAllCategory()
                                }
                            }
                        }
                    }
                    toaster(applicationContext,"Getting Images From Local",2)

                    adapter = VerticalAdapter(this@AllCategories,response.body()?.data?.tags!!,response.body()?.data?.data!!,false)
                    recyclerView.adapter = adapter
                }
                else{
                    Log.e("Failed Response", "Failed to fetch posts: ${response.code()}")
                    toaster(applicationContext,"Failed to fetch posts from API",2)
                    getFetchAllCategory1()
                }
            }

            override fun onFailure(call: Call<MainData>, t: Throwable) {
                Log.e("Failure msg", "Failed to fetch posts",t)
                toaster(applicationContext,"Failed to fetch posts from API",2)
                getFetchAllCategory1()
            }

        })

    }
    private fun downloadImage(url: String,fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var attempts = 0
            var success = false
            while (attempts < 3 && !success) {
                attempts++
                try {
            val request = Request.Builder().url(url).build()
            Log.e("download image",fileName)
            val response = client.newCall(request).execute()
            val inputStream = response.body?.byteStream()

            inputStream?.let {
                saveImageToDisk(it, fileName)
                success = true
            }
                } catch (e: Exception) {
                    Log.e("Download Error", "Attempt $attempts failed", e)
                    delay(2000) // Delay before retrying
                }
            }
            if (!success) {
                Log.e("Download Error", "Failed to download image after $attempts attempts")
            }

        }
    }
    private fun saveImageToDisk(inputStream: InputStream, fileName: String) {
        val directory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(directory, fileName)
        val outputStream = FileOutputStream(file)

        try {
            inputStream.copyTo(outputStream)
        }
        catch (e: Exception) {
            Log.e("Save Image Error", "Failed to save image to disk", e)
        }finally {
            outputStream.close()
            inputStream.close()
        }
    }
}