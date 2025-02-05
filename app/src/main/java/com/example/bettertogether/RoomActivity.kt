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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

import com.bumptech.glide.Glide

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

class RoomActivity : BaseActivity() {
    private lateinit var roomNameTextView: TextView            // -----  details  -----
    private lateinit var roomTypeTextView: TextView            // details
    private lateinit var participantsCountTextView: TextView   // details
    private lateinit var roomPointsTextView: TextView          // details
    private lateinit var roomDescriptionTextView: TextView     // details
    private lateinit var roomExpirationTextView: TextView      // details
    private lateinit var roomIsPublicTextView: TextView        // details
    private lateinit var betNowButton : Button                 // details
    private lateinit var closeBetButton : Button               // details
    private lateinit var roomImage : ImageView                 // details
    private lateinit var chatRecyclerView: RecyclerView        // -----  chat  -----
    private lateinit var messageInput: EditText                // chat
    private lateinit var sendButton: Button                    // chat
    private lateinit var joinButton: Button                    // -----  both  -----
    private lateinit var codeInput: EditText                   // both

    private var isParticipant: Boolean = false
    private var roomId: String = "null"

    private var betPoints: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        roomId = intent.getStringExtra("roomId").toString()

        joinButton = findViewById(R.id.joinButton)
        codeInput = findViewById(R.id.codeInput)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val adapter = AdapterRoomPager(this)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentView = adapter.getPageView(position)
                if (currentView != null) {
                    when (position) {
                        0 -> initRoomDetailsView(currentView)
                        1 -> initChatView(currentView)
                    }
                }
            }
        })
    }
    private fun getRoom() {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isActive = document.getBoolean("isActive") == true
                    if(!isActive){
                        toast("Room not active")
                        finish()
                    }

                    val isPublic = document.getBoolean("isPublic") == true
                    val roomsParticipants = getActiveParticipants(document)

                    val currentUser = auth.currentUser
                    isParticipant = currentUser != null && roomsParticipants[currentUser.uid]?.get("isActive") == true

                    invalidateOptionsMenu()
                    if (!isParticipant) {
                        if(isPublic){ showJoinButton(roomId) }
                        else{ showCodeInput(roomId) }
                    }

                    if (auth.currentUser == null) {
                        joinSendsToLogin()
                    }
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

    private fun initRoomDetailsView(view: View) {
        roomNameTextView = view.findViewById(R.id.roomNameText)
        roomTypeTextView = view.findViewById(R.id.roomTypeText)
        roomPointsTextView = view.findViewById(R.id.betPointsText)
        roomDescriptionTextView = view.findViewById(R.id.roomDescriptionText)
        participantsCountTextView = view.findViewById(R.id.participantsCount)
        roomExpirationTextView = view.findViewById(R.id.roomExpirationText)
        roomIsPublicTextView = view.findViewById(R.id.isPublicText)
        betNowButton = view.findViewById(R.id.betNowButton)
        closeBetButton = view.findViewById(R.id.closeBetButton)
        roomImage = view.findViewById(R.id.roomImage)

        if (roomId == "null") {
            toast("Room ID is missing")
            finish()
        }
        getRoom()
        showRoomDetails()
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
        codeInput.visibility = View.VISIBLE

        joinButton.setOnClickListener {
            val enteredCode = codeInput.text.toString()
            if (enteredCode.isBlank()) {
                toast("Please enter the room code.")
            } else {
                joinRoom(roomId, enteredCode)
            }
        }
    }
    private fun joinSendsToLogin(){
        joinButton.setOnClickListener {
            navigateToLogin()
        }
    }
    private fun joinRoom(roomId: String, enteredCode: String?) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                val roomCode = document.getString("code")
                val isPublic = document.getBoolean("isPublic") == true
                if (!isPublic && roomCode != enteredCode) {
                    toast("Incorrect room code.")
                    return@addOnSuccessListener
                }
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    toast("User not authenticated")
                    return@addOnSuccessListener
                }
                val userId = currentUser.uid
                val userName = currentUser.displayName ?: currentUser.email ?: "Anonymous"
                val userUrl = currentUser.photoUrl

                val participants = getActiveParticipants(document).toMutableMap()
                if (participants.containsKey(userId)){  // המשתמש כבר קיים - עדכן את isActive ל- true
                    val updatedParticipant = participants[userId]?.toMutableMap() ?: mutableMapOf()
                    updatedParticipant["isActive"] = true
                    participants[userId] = updatedParticipant
                    db.collection("rooms").document(roomId).update("participants", participants)
                        .addOnSuccessListener {
                            updateUserRoomEntry(userId, roomId, true)
                        }
                        .addOnFailureListener { toast("Failed to reactivate user in room") }
                } else {
                    val participantData = mapOf(
                        "name" to userName,
                        "role" to "participant",
                        "photoUrl" to userUrl,
                        "isBetting" to false,
                        "joinedOn" to System.currentTimeMillis(),
                        "isActive" to true
                    )
                    addUserToRoom(roomId, userId, participantData) { success ->
                        if (success) {
                            val betSubject = (document.getString("name") ?: "Unnamed Room")
                            addRoomToUser(userId, roomId, betSubject, "participant", isPublic) { success2 ->
                                if (success2) {
                                    toast("User added successfully")
                                    recreate()
                                }
                                else{ toast("Failed to add room from user") }
                            }
                        } else{ toast("Failed to add user from room") }
                    }
                }
            }
            .addOnFailureListener { exception -> toast("Error joining room: ${exception.message}") }
    }
    private fun updateUserRoomEntry(userId: String, roomId: String, isActive: Boolean) {
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { userDoc ->
            val rooms = getUserActiveRooms(userDoc).toMutableList()
            val roomIndex = rooms.indexOfFirst { it["roomId"] == roomId }
            if (roomIndex != -1) {
                val updatedRoomEntry = rooms[roomIndex].toMutableMap()
                updatedRoomEntry["isActive"] = isActive
                rooms[roomIndex] = updatedRoomEntry
                userRef.update("rooms", rooms)
                    .addOnSuccessListener { toast("User room reactivated") }
                    .addOnFailureListener { toast("Failed to reactivate room in user data") }
            }
        }
    }
    private fun showRoomDetails() {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    betPoints = document.getLong("betPoints") ?: 0
                    roomNameTextView.text = document.getString("name") ?: "N/A"
                    roomTypeTextView.text = document.getString("betType") ?: "N/A"
                    roomPointsTextView.text = document.getLong("betPoints")?.toString() ?: "N/A"
                    roomDescriptionTextView.text = document.getString("description") ?: "N/A"
                    roomExpirationTextView.text = document.getString("expiration") ?: "N/A"

                    val isPublic = document.getBoolean("isPublic") == true
                    roomIsPublicTextView.text = if (isPublic) "Public" else "Private"

                    val roomsParticipants = getActiveParticipants(document)
                    val numParticipants = roomsParticipants.size
                    val maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 10
                    participantsCountTextView.text = "$numParticipants/$maxParticipants"

                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val userId = currentUser.uid
                        val userParticipant = roomsParticipants[userId]
                        val roomOwnerId = document.getString("createdBy") ?: "None"
                        val isRoomOwner : Boolean = roomOwnerId == userId
                        if(isRoomOwner){
                            closeBetButton.visibility = View.VISIBLE
                            closeBetButton.setOnClickListener{ showCloseBet() }
                        }
                        val hasAlreadyBet = userParticipant?.get("isBetting") as? Boolean ?: false
                        if(hasAlreadyBet){ betNowButton.visibility = View.GONE }
                        else{ betNowButton.setOnClickListener{ showBetOptions() } }
                    }

                    val participantsViewPager = findViewById<ViewPager2>(R.id.participantsViewPager)
                    val dotsIndicator = findViewById<DotsIndicator>(R.id.participantsDotsIndicator)
                    val adapter = AdapterParticipantsPager(roomsParticipants.values.toList())
                    participantsViewPager.adapter = adapter
                    dotsIndicator.attachTo(participantsViewPager)

                    val imageUrl: String = document.getString("url") ?: "https://example.com/image.jpg"
                    loadImageFromURL(imageUrl, roomImage)
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
    private fun showBetOptions() {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val pollOptions = document.get("options") as? List<String> ?: emptyList()
                    if (pollOptions.isEmpty()) {
                        toast("No options available for betting.")
                        return@addOnSuccessListener
                    }

                    var selectedOption: String? = null

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Choose your bet")

                    builder.setSingleChoiceItems(pollOptions.toTypedArray(), -1) { _, which ->
                        selectedOption = pollOptions[which]
                    }

                    builder.setPositiveButton("Confirm") { _, _ ->
                        if (selectedOption != null) {
                            joinBet(selectedOption!!)
                        } else {
                            toast("Please select an option.")
                        }
                    }

                    builder.setNegativeButton("Cancel", null)

                    val dialog = builder.create()
                    dialog.show()
                }
            }
            .addOnFailureListener {
                toast("Error fetching betting options.")
            }
    }
    private fun joinBet(selectedOption: String) {
        val currentUser = auth.currentUser ?: run {
            toast("User not authenticated")
            return
        }
        val userId = currentUser.uid

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val userPoints = userDoc.getLong("currentPoints") ?: 0
                if (userPoints < betPoints) {
                    toast("You don't have enough points to join this bet")
                    return@addOnSuccessListener
                }
                val newPoints = userPoints - betPoints
                db.collection("users").document(userId)
                    .update("currentPoints", newPoints)
                    .addOnSuccessListener {
                        betNowButton.visibility = View.GONE
                        toast("You have joined the bet! Remaining points: $newPoints")
                        addUserToBet(selectedOption)
                    }
                    .addOnFailureListener {
                        toast("Error updating points.")
                    }
            }
            .addOnFailureListener {
                toast("Error fetching user details.")
            }
    }
    private fun addUserToBet(selectedOption: String) {
        val currentUser = auth.currentUser ?: run {
            toast("User not authenticated")
            return
        }
        val userId = currentUser.uid

        val updates = mapOf(
            "participants.$userId.isBetting" to true,           // סימון שהמשתמש מהמר
            "participants.$userId.betOption" to selectedOption  // שמירת האפשרות שבחר
        )

        db.collection("rooms").document(roomId)
            .update(updates)
            .addOnSuccessListener { toast("You have joined the bet with option: $selectedOption!") }
            .addOnFailureListener { e -> toast("Failed to update bet status: ${e.message}") }
    }
    private fun showCloseBet() {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    toast("Room not found")
                    return@addOnSuccessListener
                }

                val roomOwnerId = document.getString("createdBy") ?: "None"
                val currentUser = auth.currentUser
                if (currentUser?.uid != roomOwnerId) {
                    toast("Only the room owner can close the bet")
                    return@addOnSuccessListener
                }

                val pollOptions = document.get("options") as? List<String> ?: emptyList()
                if (pollOptions.isEmpty()) {
                    toast("No options available for closing the bet.")
                    return@addOnSuccessListener
                }

                var selectedWinningOption: String? = null

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Select the winning option")

                builder.setSingleChoiceItems(pollOptions.toTypedArray(), -1) { _, which ->
                    selectedWinningOption = pollOptions[which]
                }

                builder.setPositiveButton("Confirm") { _, _ ->
                    if (selectedWinningOption != null) {
                        processBetResults(document, selectedWinningOption!!)
                    } else {
                        toast("Please select a winning option.")
                    }
                }

                builder.setNegativeButton("Cancel", null)

                val dialog = builder.create()
                dialog.show()
            }
            .addOnFailureListener {
                toast("Error fetching room details.")
            }
    }
    private fun processBetResults(document: DocumentSnapshot, winningOption: String) {
        val roomsParticipants = getActiveParticipants(document)
        val betPoints = document.getLong("betPoints") ?: 0

        val winners = roomsParticipants.filter { (_, data) -> data["betOption"] == winningOption }
        val losers = roomsParticipants.filter { (_, data) -> data["betOption"] != winningOption }

        if (losers.isEmpty()) {
            toast("No losers found, cannot distribute winnings.")
            return
        }

        val totalPot = roomsParticipants.size * betPoints
        val rewardPerWinner = totalPot / losers.size

        val batch = db.batch()

        winners.forEach { (userId, _) ->
            val userRef = db.collection("users").document(userId)
            batch.update(userRef, "currentPoints", FieldValue.increment(rewardPerWinner.toDouble()))
        }

        val roomRef = db.collection("rooms").document(roomId)
        batch.update(roomRef, "status", "closed") // מעדכן שהחדר נסגר

        batch.commit()
            .addOnSuccessListener {
                toast("Bet closed. Winners received $rewardPerWinner points.")
                deleteRoom(roomId) // מוחק את החדר
            }
            .addOnFailureListener {
                toast("Failed to process bet results.")
            }
    }

    private fun initChatView(view: View) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString()
            if (messageText.isNotBlank()) {
                sendMessage(messageText)
            } else {
                toast("Message cannot be empty.")
            }
        }
        getRoom()
        setupChat()
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setupChat() {
        val messages = mutableListOf<Message>()
        val chatAdapter = AdapterChat(messages)

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
    private fun sendMessage(messageText: String) {
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
        leaveRoomItem?.isVisible = isParticipant
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
                    val roomsParticipants = getActiveParticipants(document)
                    val participantToRemove = roomsParticipants[userId]
                    if (participantToRemove == null) {
                        toast("User not found in participants.")
                        return@addOnSuccessListener
                    }
                    removeUserFromRoom(roomId, userId){ success ->
                        if (success) {
                            removeRoomFromUser(userId, roomId){ success2 ->
                                if (success2) {   // Now remove the room from the user's rooms array
                                    if(roomsParticipants.size <= 1){ deleteRoom(roomId) } // Delete the room if no participants are left
                                    toast("You have left the room")
                                    navigateTo(RatingActivity::class.java)
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

class AdapterParticipantsPager(private val participants: List<Map<String, Any>>) :
    RecyclerView.Adapter<AdapterParticipantsPager.ParticipantViewHolder>() {
    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.participantName)
        val roleTextView: TextView = itemView.findViewById(R.id.participantRole)
        val profileImageView: ImageView = itemView.findViewById(R.id.participantImage)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }
    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = participants[position]
        holder.nameTextView.text = participant["name"] as? String ?: "Unknown"
        holder.roleTextView.text = participant["role"] as? String ?: "No Role"
        var imageUrl  = participant["photoUrl"]
        Glide.with(holder.profileImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.profileImageView)
    }
    override fun getItemCount(): Int = participants.size
}

data class Message(
    val sender: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
class AdapterChat(private val messages: List<Message>) :
    RecyclerView.Adapter<AdapterChat.ChatViewHolder>() {
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderTextView: TextView = itemView.findViewById(R.id.sender_text)
        val messageTextView: TextView = itemView.findViewById(R.id.message_text)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.senderTextView.text = message.sender
        holder.messageTextView.text = message.message
    }
    override fun getItemCount(): Int {
        return messages.size
    }
}

class AdapterRoomPager(private val context: Context) : RecyclerView.Adapter<AdapterRoomPager.ViewHolder>() {
    private val pageViews = mutableMapOf<Int, View>() // שמירה על Views לפי position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val layout = when (viewType) {
            0 -> R.layout.page_room_details // עמוד פרטי החדר
            1 -> R.layout.page_chat         // עמוד הצ'אט
            else -> throw IllegalStateException("Invalid view type")
        }
        val view = inflater.inflate(layout, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        pageViews[position] = holder.itemView // שמירת ה-View במפה
    }
    fun getPageView(position: Int): View? = pageViews[position] // גישה לעמוד לפי position
    override fun getItemViewType(position: Int): Int = position // קביעת סוג העמוד
    override fun getItemCount(): Int = 2 // שני עמודים בלבד
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
