package com.example.bettertogether

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class RoomActivity : BaseActivity() {
    private lateinit var roomNameTextView: TextView
    private lateinit var roomTypeTextView: TextView
    private lateinit var participantsCountTextView: TextView
    private lateinit var roomPointsTextView: TextView
    private lateinit var roomDescriptionTextView: TextView
    private lateinit var roomExpirationTextView: TextView
    private lateinit var roomIsPublicTextView: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var joinButton: Button
    private lateinit var codeInput: EditText
    private lateinit var betNowButton : Button
    private lateinit var roomImage : ImageView

    private var isParticipant: Boolean = false
    private var roomId: String = "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        roomId = intent.getStringExtra("roomId").toString()

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val adapter = AdapterRoomPager(this)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentView = adapter.getPageView(position)
                if (currentView != null) {
                    when (position) {
                        0 -> initRoomDetailsView(currentView) // אתחול עמוד פרטי החדר
                        1 -> initChatView(currentView)        // אתחול עמוד הצ'אט
                    }
                }
            }
        })

        adapter.getPageView(0)?.let { initRoomDetailsView(it) }
        adapter.getPageView(1)?.let { initChatView(it) }
    }

    private fun initRoomDetailsView(view: View) {
        roomNameTextView = view.findViewById(R.id.roomNameText)
        roomTypeTextView = view.findViewById(R.id.roomTypeText)
        roomPointsTextView = view.findViewById(R.id.betPointsText)
        roomDescriptionTextView = view.findViewById(R.id.roomDescriptionText)
        participantsCountTextView = view.findViewById(R.id.participantsCount)
        roomExpirationTextView = view.findViewById(R.id.roomExpirationText)
        roomIsPublicTextView = view.findViewById(R.id.isPublicText)
        joinButton = view.findViewById(R.id.joinButton)
        betNowButton = view.findViewById(R.id.betNowButton)
        codeInput = view.findViewById(R.id.codeInput)
        roomImage = view.findViewById(R.id.roomImage)

        if (roomId != "null") {
            fetchRoomDetails(roomId) { isPublic, participantStatus ->
                isParticipant = participantStatus
                if(!isParticipant){
                    if(isPublic){ showJoinButton(roomId) }
                    else{ showCodeInput(roomId) }
                }
                if(auth.currentUser == null){ hideRoomFunctions() }
                invalidateOptionsMenu()
            }
        } else {
            toast("Room ID is missing")
            finish()
        }
    }
    private fun hideRoomFunctions(){
        joinButton.setOnClickListener {
            navigateToLogin()
        }
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

                    val roomsParticipants = document.get("participants") as? List<Map<String, Any>> ?: emptyList()
                    val numPaticipants = roomsParticipants.size ?: 1
                    val maxParticipants = document.getString("maxParticipants")?.toIntOrNull() ?: 10
                    participantsCountTextView.text = "$numPaticipants/$maxParticipants"

                    val participantsViewPager = findViewById<ViewPager2>(R.id.participantsViewPager)
                    val dotsIndicator = findViewById<DotsIndicator>(R.id.participantsDotsIndicator)
                    val adapter = AdapterParticipantsPager(roomsParticipants)
                    participantsViewPager.adapter = adapter
                    dotsIndicator.attachTo(participantsViewPager)

                    val imageUrl : String = document.getString("url") ?: "https://example.com/image.jpg"
                    loadImageFromURL(imageUrl,roomImage)

                    // Check if the user is a participant
                    val currentUser = auth.currentUser
                    val isParticipant = currentUser != null && roomsParticipants.any { it["id"] == currentUser.uid }
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
    private fun showJoinButton(roomId: String) {
        betNowButton.visibility = View.GONE
        joinButton.visibility = View.VISIBLE
        codeInput.visibility = View.GONE

        joinButton.setOnClickListener {
            joinRoom(roomId, null)
        }
    }
    private fun showCodeInput(roomId: String) {
        betNowButton.visibility = View.GONE
        joinButton.visibility = View.VISIBLE
//        codeInput.visibility = View.VISIBLE

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
                val userUrl = currentUser.photoUrl

                val participantData = mapOf(
                    "id" to userId,
                    "name" to userName,
                    "role" to "participant",
                    "photoUrl" to userUrl,
                    "joinedOn" to System.currentTimeMillis()
                )

                // Add to participants array in the room document
                addUserToRoom(roomId, participantData){ success ->
                    if (success) {
                        val betSubject = (document.getString("name") ?: "Unnamed Room")
                        addRoomToUser(userId,roomId,betSubject,"participant",isPublic){ success2 ->
                            if (success2) {
                                toast("User added successfully")
                                recreate() // Refresh the activity to show the chat
                            }
                            else { toast("Failed to add room from user") }
                        }
                    } else { toast("Failed to add user from room") }
                }
            }
            .addOnFailureListener { exception -> toast("Error joining room: ${exception.message}") }
    }

    private fun initChatView(view: View) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString()
            if (messageText.isNotBlank()) {
                sendMessage(roomId, messageText)
            } else {
                toast("Message cannot be empty.")
            }
        }
        setupChat(roomId)
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setupChat(roomId: String) {
        val messages = mutableListOf<Message>()
        val chatAdapter = AdapterChat(messages)

        // אתחול RecyclerView לצ'אט
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

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
                    removeUserFromRoom(roomId, participantToRemove){ success ->
                        if (success) {
                            removeRoomFromUser(userId, roomId){ success2 ->
                                if (success2) {   // Now remove the room from the user's rooms array
                                    if(participants.size <= 1){ deleteRoom(roomId) } // Delete the room if no participants are left
                                    toast("You have left the room")
                                    navigateTo(RoomsActivity::class.java)
                                }
                                else { toast("Failed to remove room from user") }
                            }
                        } else { toast("Failed to remove user from room") }
                    }
                }
                .addOnFailureListener { exception -> toast("Error retrieving room data: ${exception.message}") }
        } ?: run { toast("Room ID is missing.") }
    }
}
