package com.example.bettertogether

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class RoomActivity : BaseActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Retrieve roomId from the intent
        val roomId = intent.getStringExtra("roomId")

        if (roomId != null) {
            fetchRoomDetails(roomId)
        } else {
            Toast.makeText(this, "Room ID is missing", Toast.LENGTH_SHORT).show()
            finish() // Close the activity if no roomId is provided
        }
        setupBottomNavigation()
    }

    private fun fetchRoomDetails(roomId: String) {
        val roomNameTextView: TextView = findViewById(R.id.room_name)
        val roomTypeTextView: TextView = findViewById(R.id.room_type)
        val roomPointsTextView: TextView = findViewById(R.id.room_points)
        val roomDescriptionTextView: TextView = findViewById(R.id.room_description)
        val roomExpirationTextView: TextView = findViewById(R.id.room_expiration)
        val roomIsPublicTextView: TextView = findViewById(R.id.room_is_public)

        firestore.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Populate the UI with the room details
                    roomNameTextView.text = document.getString("name") ?: "N/A"
                    roomTypeTextView.text = document.getString("betType") ?: "N/A"
                    roomPointsTextView.text = document.getString("betPoints") ?: "N/A"
                    roomDescriptionTextView.text = document.getString("description") ?: "N/A"
                    roomExpirationTextView.text = document.getString("expiration") ?: "N/A"
                    roomIsPublicTextView.text =
                        if (document.getBoolean("isPublic") == true) "Public" else "Private"
                } else {
                    Toast.makeText(this, "Room not found", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity if the room doesn't exist
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching room details: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish() // Close the activity on failure
            }
    }
}
