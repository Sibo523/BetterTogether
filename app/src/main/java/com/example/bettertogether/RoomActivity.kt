package com.example.bettertogether

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue

import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlin.concurrent.thread

class RoomActivity : BaseActivity() {

    private lateinit var progressDialog: ProgressDialog
    private val mainHandler = Handler(Looper.getMainLooper())

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
    private lateinit var roomImage : ImageView

    private var isParticipant: Boolean = false
    private var roomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        // Initialize views
        initViews()

        roomId = intent.getStringExtra("roomId")
        val roomId = roomId
        if (roomId != null) {
            fetchRoomDetails(roomId) { isPublic, participantStatus ->
                isParticipant = participantStatus
                if(isParticipant){ setupChat(roomId) }
                else{
                    if(isPublic){ showJoinButton(roomId) }
                    else{ showCodeInput(roomId) }
                }
                invalidateOptionsMenu()
            }
        } else {
            toast("Room ID is missing")
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
        roomImage = findViewById(R.id.roomImgViaURL)
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
                    toast("Room not found")
                    finish()
                }
            }
            .addOnFailureListener {
                toast("Error fetching room details.")
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
                    toast("Error loading messages.")
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
                toast("Message cannot be empty.")
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
                toast("Error sending message.")
            }
    }

    private fun showJoinButton(roomId: String) {
        joinButton.visibility = View.VISIBLE
        chatRecyclerView.visibility = View.INVISIBLE
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
        chatRecyclerView.visibility = View.INVISIBLE
        messageInput.visibility = View.GONE
        sendButton.visibility = View.GONE

        joinButton.setOnClickListener {
            val enteredCode = codeInput.text.toString()
            if (enteredCode.isBlank()) {
                toast("Please enter the room code.")
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
                if (!isPublic && roomCode != enteredCode) {
                    toast("Incorrect room code.")
                }
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    toast("User not authenticated")
                    return@addOnSuccessListener
                }
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
                        val betSubject = (document.getString("name") ?: "Unnamed Room")
                        addRoomToUser(userId,roomId,betSubject,"participant",isPublic)
                    }
                    .addOnFailureListener { exception -> toast("Failed to join room: ${exception.message}") }
            }
            .addOnFailureListener { exception -> toast("Error joining room: ${exception.message}") }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.room_menu, menu)
        val leaveRoomItem = menu.findItem(R.id.action_leave_room)
        leaveRoomItem.isVisible = isParticipant
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_leave_room -> {
                showLeaveRoomDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showLeaveRoomDialog() {
        AlertDialog.Builder(this)
            .setTitle("Leave Room")
            .setMessage("Are you sure you want to leave this room?")
            .setPositiveButton("Leave") { _, _ -> leaveRoom() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveRoom() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: return

        roomId?.let { roomId ->
            // First, retrieve the current user and room data to ensure correct object removal
            db.collection("rooms").document(roomId).get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        toast("Room not found.")
                        return@addOnSuccessListener
                    }
                    val participants = document.get("participants") as? List<Map<String,Any>> ?: emptyList()
                    val participantToRemove = participants.find { it["id"] == userId }  // Find the exact participant object to remove
                    if (participantToRemove == null) {
                        toast("User not found in participants.")
                        return@addOnSuccessListener
                    }
                    db.collection("rooms").document(roomId)  // Remove the user from the room's participants array
                        .update("participants", FieldValue.arrayRemove(participantToRemove))
                        .addOnSuccessListener {
                            removeRoomFromUser(userId, roomId)  // Now remove the room from the user's rooms array
                            if(participants.size == 1){ deleteRoom(roomId) } // Delete the room if no participants are left
                            else{ toast("You have left the room.") }
                            finish()
                        }
                        .addOnFailureListener { exception -> toast("Error updating room data: ${exception.message}") }
                }
                .addOnFailureListener { exception -> toast("Error retrieving room data: ${exception.message}") }
        } ?: run { toast("Room ID is missing.") }
    }

//    private fun loadImageFromURL(imageUrl: String) {
//        progressDialog = ProgressDialog(this)
//        progressDialog.setMessage("Getting your pic....")
//        progressDialog.setCancelable(false)
//        progressDialog.show()
//
//        // Perform network operation on a separate thread
//        thread {
//            var bitmap: Bitmap? = null
//            try {
//                val inputStream: InputStream = URL(imageUrl).openStream()
//                bitmap = BitmapFactory.decodeStream(inputStream)
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//
//            // Update UI on the main thread
//            mainHandler.post {
//                progressDialog.dismiss()
//                if (bitmap != null) {
//                    roomImage.setImageBitmap(bitmap)
//                } else {
//                    Toast.makeText(
//                        this@RoomActivity,
//                        "Failed to load image",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

}