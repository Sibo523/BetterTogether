package com.example.bettertogether

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class AdapterRoomPager(private val context: Context) : RecyclerView.Adapter<AdapterRoomPager.ViewHolder>() {

    // מספר העמודים
    override fun getItemCount(): Int = 2

    // יצירת ה-ViewHolder לכל עמוד
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val layout = when (viewType) {
            0 -> R.layout.page_room_details  // עמוד פרטי חדר
            1 -> R.layout.page_chat          // עמוד צ'אט
            else -> throw IllegalStateException("Invalid view type")
        }
        val view = inflater.inflate(layout, parent, false)
        return ViewHolder(view)
    }

    // קביעת סוג ה-View לכל עמוד
    override fun getItemViewType(position: Int): Int = position

    // חיבור ה-ViewHolder לתוכן
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // אם יש צורך בנתונים דינמיים, אפשר למלא אותם כאן לפי `position`
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
