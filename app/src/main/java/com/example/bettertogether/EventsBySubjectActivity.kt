package com.example.bettertogether

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EventsBySubjectActivity : BaseActivity() {

    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var subjectTitle: TextView
    private lateinit var eventsAdapter: AdapterEvents
    private val eventsList = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events_by_subject)

        subjectTitle = findViewById(R.id.subject_title)
        eventsRecyclerView = findViewById(R.id.events_recycler_view)

        val subject = intent.getStringExtra("subject") ?: "Events"
        subjectTitle.text = subject

        eventsAdapter = AdapterEvents(eventsList) { event ->
            // כאן ניתן להוסיף מעבר לעמוד הפרטים של האירוע
        }
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsRecyclerView.adapter = eventsAdapter

        loadEvents(subject)
    }

    private fun loadEvents(subject: String) {
        db.collection("rooms")
            .whereEqualTo("isEvent", true)
            .whereEqualTo("eventSubject", subject)
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
