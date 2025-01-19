package com.example.bettertogether

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AdapterRoomPager(private val context: Context) : RecyclerView.Adapter<AdapterRoomPager.ViewHolder>() {

    private val pageViews = mutableMapOf<Int, View>() // שמירה על Views לפי position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val layout = when (viewType) {
            0 -> R.layout.page_room_details // עמוד פרטי החדר
            1 -> R.layout.page_chat         // עמוד הצ'אט
            else -> throw IllegalStateException("Invalid view type")
        }
        val view = inflater.inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        pageViews[position] = holder.itemView // שמירת ה-View במפה
    }

    fun getPageView(position: Int): View? = pageViews[position] // גישה לעמוד לפי position

    override fun getItemViewType(position: Int): Int = position // קביעת סוג העמוד

    override fun getItemCount(): Int = 2 // שני עמודים בלבד

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
