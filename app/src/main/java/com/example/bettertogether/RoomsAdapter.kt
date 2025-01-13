package com.example.bettertogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoomsAdapter(
    private val rooms: List<Triple<String, String, Boolean>>, // Triple<roomID, roomName, isPrivate>
    private val onRoomClick: (String) -> Unit // Callback for handling clicks
) : RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>() {

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameTextView: TextView = view.findViewById(R.id.room_name)
        val lockIconImageView: ImageView = view.findViewById(R.id.lock_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val (_, roomName, isPublic) = rooms[position] // Extract roomName and isPrivate from the Triple
        holder.roomNameTextView.text = roomName

        // Show or hide lock icon based on isPrivate
        holder.lockIconImageView.visibility = if (isPublic) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onRoomClick(rooms[position].first) // Pass roomID to the callback
        }
    }

    override fun getItemCount(): Int {
        return rooms.size
    }
}
