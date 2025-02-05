package com.example.bettertogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot

class AdapterRooms(
    private var rooms: MutableList<DocumentSnapshot>,
    private val onRoomClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdapterRooms.RoomViewHolder>() {

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameTextView: TextView = view.findViewById(R.id.room_name)
        val participantsCounterTextView: TextView = view.findViewById(R.id.participants_count)
        val lockIconImageView: ImageView = view.findViewById(R.id.lock_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val roomDocument = rooms[position]
        val roomName = roomDocument.getString("name") ?: "Unnamed Room"

        // Get the number of active participants
        val roomsParticipants = roomDocument.get("participants") as? Map<String, Map<String, Any>> ?: emptyMap()
        val activeParticipants = roomsParticipants.filterValues { it["isActive"] == true }
        val maxParticipants = roomDocument.getLong("maxParticipants")?.toInt() ?: 10

        val isPublic = roomDocument.getBoolean("isPublic") ?: false

        holder.roomNameTextView.text = roomName
        holder.participantsCounterTextView.text = "${activeParticipants.size}/$maxParticipants"
        holder.lockIconImageView.visibility = if (isPublic) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onRoomClick(roomDocument)
        }
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    fun updateData(newRooms: List<DocumentSnapshot>) {
        rooms.clear()
        rooms.addAll(newRooms)
        notifyDataSetChanged()
    }
}
