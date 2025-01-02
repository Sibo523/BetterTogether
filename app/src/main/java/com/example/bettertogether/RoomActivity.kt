package com.example.bettertogether

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue

class RoomActivity : BaseActivity() {

    private lateinit var roomNameTextView: TextView
    private lateinit var roomTypeTextView: TextView
    private lateinit var roomPointsTextView: TextView
    private lateinit var roomDescriptionTextView: TextView
    private lateinit var roomExpirationTextView: TextView
    private lateinit var roomIsPublicTextView: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var joinButton: Button
    private lateinit var codeInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        // Initialize views
        initViews()

        val roomId = intent.getStringExtra("roomId")
        if (roomId != null) {
            fetchRoomDetails(roomId) { isPublic, isParticipant ->
                if (isParticipant) {
                    setupChat(roomId)
                } else {
                    if (isPublic) {
                        showJoinButton(roomId)
                    } else {
                        showCodeInput(roomId)
                    }
                }
            }
        } else {
            Toast.makeText(this, "Room ID is missing", Toast.LENGTH_SHORT).show()
            finish()
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
        joinButton = findViewById(R.id.join_button)
        codeInput = findViewById(R.id.code_input)
    }

    private fun fetchRoomDetails(roomId: String, callback: (Boolean, Boolean) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Populate UI with room details
                    roomNameTextView.text = document.getString("name") ?: "N/A"
                    roomTypeTextView.text = document.getString("betType") ?: "N/A"
                    roomPointsTextView.text = document.getString("betPoints") ?: "N/A"
                    roomDescriptionTextView.text = document.getString("description") ?: "N/A"
                    roomExpirationTextView.text = document.getString("expiration") ?: "N/A"
                    val isPublic = document.getBoolean("isPublic") == true
                    roomIsPublicTextView.text = if (isPublic) "Public" else "Private"

                    // Check if the user is a participant
                    val participants =
                        document.get("participants") as? List<Map<String, Any>> ?: emptyList()
                    val currentUser = auth.currentUser
                    val isParticipant =
                        currentUser != null && participants.any { it["id"] == currentUser.uid }
                    callback(isPublic, isParticipant)
                } else {
                    Toast.makeText(this, "Room not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching room details.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupChat(roomId: String) {
        val messages = mutableListOf<Message>()
        val chatAdapter = ChatAdapter(messages)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        db.collection("rooms").document(roomId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messages.clear()
                    for (doc in snapshots) {
                        messages.add(doc.toObject(Message::class.java))
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }

        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString()
            if (messageText.isNotBlank()) {
                sendMessage(roomId, messageText)
            } else {
                Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(roomId: String, messageText: String) {
        val currentUser = auth.currentUser
        val senderName = currentUser?.displayName ?: currentUser?.email ?: "Anonymous"

        val message = Message(sender = senderName, message = messageText)
        db.collection("rooms").document(roomId).collection("messages")
            .add(message)
            .addOnSuccessListener {
                messageInput.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error sending message.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showJoinButton(roomId: String) {
        joinButton.visibility = View.VISIBLE
        chatRecyclerView.visibility = View.GONE
        messageInput.visibility = View.GONE
        sendButton.visibility = View.GONE
        codeInput.visibility = View.GONE

        joinButton.setOnClickListener {
            joinRoom(roomId, null)
        }
    }

    private fun showCodeInput(roomId: String) {
        joinButton.visibility = View.VISIBLE
        codeInput.visibility = View.VISIBLE
        chatRecyclerView.visibility = View.GONE
        messageInput.visibility = View.GONE
        sendButton.visibility = View.GONE

        joinButton.setOnClickListener {
            val enteredCode = codeInput.text.toString()
            if (enteredCode.isBlank()) {
                Toast.makeText(this, "Please enter the room code.", Toast.LENGTH_SHORT).show()
            } else {
                joinRoom(roomId, enteredCode)
            }
        }
    }

    private fun joinRoom(roomId: String, enteredCode: String?) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                val roomCode = document.getString("code")
                val isPublic = document.getBoolean("isPublic") == true
                if (isPublic || roomCode == enteredCode) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val userId = currentUser.uid
                        val userName = currentUser.displayName ?: currentUser.email ?: "Anonymous"

                        val participantData = mapOf(
                            "id" to userId,
                            "name" to userName,
                            "role" to "participant",
                            "joinedOn" to System.currentTimeMillis()
                        )

                        // Add to participants array in the room document
                        db.collection("rooms").document(roomId)
                            .update("participants", FieldValue.arrayUnion(participantData))
                            .addOnSuccessListener {
                                // Add to user's rooms array
                                val roomData = mapOf(
                                    "roomId" to roomId,
                                    "roomName" to (document.getString("name") ?: "Unnamed Room"),
                                    "joinedOn" to System.currentTimeMillis(),
                                    "role" to "participant"
                                )
                                db.collection("users").document(userId)
                                    .update("rooms", FieldValue.arrayUnion(roomData))
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Successfully joined the room!", Toast.LENGTH_SHORT).show()
                                        recreate() // Refresh the activity to show the chat
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(this, "Failed to update user room data: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Failed to join room: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Incorrect room code.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error joining room: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

}