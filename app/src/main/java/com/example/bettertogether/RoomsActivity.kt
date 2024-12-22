package com.example.bettertogether

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoomsActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: RoomsAdapter
    private val roomsList = mutableListOf<Pair<String, String>>() // Pair<roomID, roomName>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rooms)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.rooms_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        roomsAdapter = RoomsAdapter(roomsList) { roomId ->
            openRoom(roomId)
        }
        recyclerView.adapter = roomsAdapter

        loadUserRooms()
        setupBottomNavigation()
    }

    private fun loadUserRooms() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please log in to see your rooms.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(user.uid).collection("rooms")
            .get()
            .addOnSuccessListener { documents ->
                roomsList.clear()
                for (document in documents) {
                    val roomId = document.id
                    db.collection("rooms").document(roomId).get()
                        .addOnSuccessListener { roomDoc ->
                            if (roomDoc.exists()) {
                                val roomName = roomDoc.getString("name") ?: "Unnamed Room"
                                roomsList.add(Pair(roomId, roomName))
                                roomsAdapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error fetching room details: ${exception.message}")
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching rooms: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
