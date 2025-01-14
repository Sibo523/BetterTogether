package com.example.bettertogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterPopularRooms(
    private val rooms: List<Map<String, Any>>,
    private val onRoomClick: (String) -> Unit
) : RecyclerView.Adapter<AdapterPopularRooms.RoomViewHolder>() {

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomName: TextView = view.findViewById(R.id.room_name)
        val participantsCount: TextView = view.findViewById(R.id.participants_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_popular_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        val roomName = room["name"] as? String ?: "Unnamed Room"
        val participantsCount = room["participantsCount"] as? Int ?: 0
        val maxParticipants = room["maxParticipants"] as? Int ?: 10 // Replace with a dynamic value if available

        holder.roomName.text = roomName
        holder.participantsCount.text = "$participantsCount/$maxParticipants"

        holder.itemView.setOnClickListener {
            val roomId = room["id"] as String
            onRoomClick(roomId) // Trigger the callback with the room ID
        }
    }

    override fun getItemCount(): Int {
        return rooms.size
    }
}
