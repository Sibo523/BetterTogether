package com.example.bettertogether

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot

class EventsBySubjectActivity : BaseActivity() {

    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var subjectTitle: TextView
    private lateinit var eventsAdapter: AdapterEvents
    private val eventsList = mutableListOf<DocumentSnapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events_by_subject)

        subjectTitle = findViewById(R.id.subject_title)
        eventsRecyclerView = findViewById(R.id.events_recycler_view)

        val subject = intent.getStringExtra("subject") ?: "Events"
        subjectTitle.text = subject

        eventsAdapter = AdapterEvents(eventsList) { event ->
            openRoom(event.id)
        }
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsRecyclerView.adapter = eventsAdapter

        loadEvents(subject)
    }

    private fun loadEvents(subject: String) {
        db.collection("rooms")
            .whereEqualTo("isEvent", true)
            .whereEqualTo("eventSubject", subject)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                eventsList.clear()
                eventsList.addAll(documents.documents)
                eventsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // הודעת שגיאה
            }
    }
}


class AdapterEvents(
    private val events: List<DocumentSnapshot>,
    private val onEventClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdapterEvents.EventViewHolder>() {
    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventImage: ImageView = view.findViewById(R.id.event_image)
        val eventName: TextView = view.findViewById(R.id.event_name)
        val eventDate: TextView = view.findViewById(R.id.event_date)
        val eventParticipantsCounter: TextView = view.findViewById(R.id.participants_count)
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
        var roomsParticipants = event.get("participants") as? Map<String, Map<String, Any>> ?: emptyMap()
        roomsParticipants = roomsParticipants.filterValues { it["isActive"] == true }
        val maxParticipants = event.getLong("maxParticipants")?.toInt() ?: 10

        holder.eventName.text = eventName
        holder.eventDate.text = eventDate
        holder.eventParticipantsCounter.text = "${roomsParticipants.size}/$maxParticipants"
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
