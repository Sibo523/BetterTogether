package com.example.bettertogether

import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoomActivity : BaseActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var roomNameTextView: TextView
    private lateinit var roomTypeTextView: TextView
    private lateinit var roomPointsTextView: TextView
    private lateinit var roomDescriptionTextView: TextView
    private lateinit var roomExpirationTextView: TextView
    private lateinit var roomIsPublicTextView: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initViews()

        val roomId = intent.getStringExtra("roomId")
        if (roomId != null) {
            fetchRoomDetails(roomId)
            setupChat(roomId)
        } else {
            Toast.makeText(this, "Room ID is missing", Toast.LENGTH_SHORT).show()
            finish() // Close the activity if no roomId is provided
        }

        setupBottomNavigation()
    }

    private fun initViews() {
        roomNameTextView = findViewById(R.id.room_name)
        roomTypeTextView = findViewById(R.id.room_type)
        roomPointsTextView = findViewById(R.id.room_points)
        roomDescriptionTextView = findViewById(R.id.room_description)
        roomExpirationTextView = findViewById(R.id.room_expiration)
        roomIsPublicTextView = findViewById(R.id.room_is_public)
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
    }

    private fun fetchRoomDetails(roomId: String) {
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

    private fun setupChat(roomId: String) {
        val messages = mutableListOf<Message>()
        val chatAdapter = ChatAdapter(messages)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        // Listen for messages in real-time
        firestore.collection("rooms").document(roomId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messages.clear()
                    for (doc in snapshots) {
                        val message = doc.toObject(Message::class.java)
                        messages.add(message)
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }

        // Send new messages
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString()
            if (messageText.isNotBlank()) {
                val currentUser = auth.currentUser
                val senderName = currentUser?.displayName ?: currentUser?.email ?: "Anonymous"

                val message = Message(
                    sender = senderName,
                    message = messageText
                )

                firestore.collection("rooms").document(roomId).collection("messages")
                    .add(message)
                    .addOnSuccessListener {
                        messageInput.text.clear()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error sending message: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
