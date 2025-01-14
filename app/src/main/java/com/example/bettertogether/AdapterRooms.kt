package com.example.bettertogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot

class AdapterRooms(
    private val rooms: List<DocumentSnapshot>, // List of Firestore document snapshots
    private val onRoomClick: (DocumentSnapshot) -> Unit // Callback for handling room clicks
) : RecyclerView.Adapter<AdapterRooms.RoomViewHolder>() {

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameTextView: TextView = view.findViewById(R.id.room_name)
        val participantsCounterTextView: TextView = view.findViewById(R.id.participants_counter)
        val lockIconImageView: ImageView = view.findViewById(R.id.lock_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val roomDocument = rooms[position]
        val roomName = roomDocument.getString("name") ?: "Unnamed Room"
        val participants = roomDocument.get("participants") as? List<*> ?: emptyList<Any>()
        val isPublic = roomDocument.getBoolean("isPublic") ?: false
        val maxParticipants = roomDocument.getString("maxParticipants")?.toIntOrNull() ?: 10

        holder.roomNameTextView.text = roomName
        holder.participantsCounterTextView.text = "${participants.size}/$maxParticipants"
        holder.lockIconImageView.visibility = if (isPublic) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onRoomClick(roomDocument) // Pass the full document snapshot to the callback
        }
    }

    override fun getItemCount(): Int {
        return rooms.size
    }
}
