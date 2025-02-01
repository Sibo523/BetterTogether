package com.example.bettertogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot

class AdapterEvents(
    private val events: List<DocumentSnapshot>,
    private val onEventClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdapterEvents.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventImage: ImageView = view.findViewById(R.id.event_image)
        val eventName: TextView = view.findViewById(R.id.event_name)
        val eventDate: TextView = view.findViewById(R.id.event_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        val eventName = event.getString("name") ?: "Unnamed Event"
        val eventDate = event.getString("expiration") ?: "No Date"
        val eventImageUrl = event.getString("url") ?: ""

        holder.eventName.text = eventName
        holder.eventDate.text = eventDate
        Glide.with(holder.itemView.context)
            .load(eventImageUrl)
            .placeholder(R.drawable.room_placeholder_image)
            .into(holder.eventImage)

        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount(): Int = events.size
}
