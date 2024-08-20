package com.game.categories.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.game.categories.model.Datum
import com.game.categories.model.Tag


class VerticalAdapter(val context:Context,val tag: List<Tag>, val data: List<Datum> , val isShop: Boolean) :
    RecyclerView.Adapter<VerticalAdapter.MyViewHolder>() {

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(com.game.categories.R.id.categoryName)
        val recyclerview: RecyclerView = view.findViewById(com.game.categories.R.id.category_recyclerview)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(com.game.categories.R.layout.vertical_recycler, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val tagCategory = tag[position].category_id.toString()
        val intList = tagCategory.split(",").map { it.toInt() }
        val dataValue: MutableList<Datum> = mutableListOf()
        val uniqueid:MutableList<Int> = mutableListOf()
        for (data1 in data) {
            if (data1.id !in uniqueid){
                uniqueid.add(data1.id!!)
                for (intItem in intList) {
                    if (isShop) {
                        if (data1.id == intItem && data1.owned == true) {
                            dataValue.add(data1)
                        }
                    } else {
                        if (data1.id == intItem) {
                            dataValue.add(data1)
                        }
                    }
                }
            }
        }
        holder.recyclerview.adapter = HorizontalAdapter(context,dataValue)

        holder.recyclerview.layoutManager =
            LinearLayoutManager(holder.recyclerview.context, LinearLayoutManager.HORIZONTAL, false)
        holder.textView.text = tag[position].tagName
    }

    override fun getItemCount(): Int {
        return tag.size
    }

}