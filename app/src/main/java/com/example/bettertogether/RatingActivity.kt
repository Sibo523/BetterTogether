package com.example.bettertogether

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class RatingActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterParticipantsPager
    private val topUsersList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        recyclerView = findViewById(R.id.rating_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdapterParticipantsPager(topUsersList)
        recyclerView.adapter = adapter

        loadTopUsers()
    }

    private fun loadTopUsers() {
        db.collection("users")
            .orderBy("currentPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                topUsersList.clear()
                topUsersList.addAll(documents.map { document ->
                    mapOf(
                        "name" to (document.getString("displayName") ?: "Unknown"),
                        "role" to "Top User",
                        "photoUrl" to (document.getString("photoUrl") ?: "")
                    )
                })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching top users: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
