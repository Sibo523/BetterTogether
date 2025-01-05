package com.example.bettertogether

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RoomsActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: RoomsAdapter
    private val roomsList = mutableListOf<Triple<String, String, Boolean>>() // Triple<roomID, roomName, isPublic>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rooms)

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

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    roomsList.clear()
                    val rooms = document.get("rooms") as? List<Map<String, Any>>// Assuming rooms is a list of maps
                    if (rooms != null) {
                        for (room in rooms) {
                            val roomId = room["roomId"] as? String ?: continue
                            val roomName = room["roomName"] as? String ?: "Unnamed Room"
                            val isPublic = room["isPublic"] as? Boolean ?: false // Default to false
                            roomsList.add(Triple(roomId, roomName, isPublic))
                        }
                        roomsAdapter.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching rooms: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
