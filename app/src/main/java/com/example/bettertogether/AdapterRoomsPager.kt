package com.example.bettertogether

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class AdapterRoomsPager(private val context: Context) : RecyclerView.Adapter<AdapterRoomsPager.ViewHolder>() {
    private val pageViews = mutableMapOf<Int, View>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val layout = when (viewType) {
            0 -> R.layout.page_explorer
            1 -> R.layout.page_rooms
            2 -> R.layout.page_users
            else -> throw IllegalStateException("Invalid view type")
        }
        val view = inflater.inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        pageViews[position] = holder.itemView
    }

    fun getPageView(position: Int): View? = pageViews[position]

    override fun getItemViewType(position: Int): Int = position

    override fun getItemCount(): Int = 3

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
