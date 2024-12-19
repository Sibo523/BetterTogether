package com.example.bettertogether
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoomsAdapter(
    private val rooms: List<String>,
    private val onRoomClick: (String) -> Unit // A callback for handling clicks
) : RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>() {

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameTextView: TextView = view.findViewById(R.id.room_name_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.room_item, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val roomName = rooms[position]
        holder.roomNameTextView.text = roomName
        holder.itemView.setOnClickListener {
            onRoomClick(roomName) // Trigger the callback when a room is clicked
        }
    }

    override fun getItemCount(): Int {
        return rooms.size
    }
}
