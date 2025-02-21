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

    private var userRole: String = "participant"
    private var userStatus: String = "client"
    private var isParticipant: Boolean = false
    private var roomId: String = "null"

    private var isPublic: Boolean = false
    private var betType: String = ""
    private var pollOptions: List<String> = emptyList()

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
                    pollOptions = document.get("options") as? List<String> ?: emptyList()

                    val isActive = document.getBoolean("isActive") == true
                    if (!isActive) {
                        toast("Room not active")
                        navigateTo(HomeActivity::class.java)
                    }
                    isPublic = document.getBoolean("isPublic") == true
                    val roomsParticipants = getActiveParticipants(document)
                    if (isLoggedIn) {
                        isParticipant = roomsParticipants[userId]?.get("isActive") == true
                        userRole = roomsParticipants[userId]?.get("role").toString()
                        getUserStatus(userId) { status -> if(status != null){ userStatus = status } }
                    }
                    if (!isParticipant) {
                        if(isPublic){ showJoinButton() }
                        else{ showCodeInput() }
                    } else{ showLeaveRoom() }
                    if(userRole == "banned" || userStatus == "banned"){
                        toast("You are banned!")
                        navigateTo(HomeActivity::class.java)
                    }
                    if(!isLoggedIn){ joinSendsToLogin() }
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
    private fun showJoinButton() {
        betNowButton.visibility = View.GONE
        joinButton.visibility = View.VISIBLE
        codeInput.visibility = View.GONE

        joinButton.setOnClickListener {
            joinRoom(roomId, null)
        }
    }
    private fun showCodeInput() {
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
    private fun showLeaveRoom() {
        leaveRoomItem.visibility = View.VISIBLE
        leaveRoomItem.setOnClickListener { showLeaveRoomDialog() }
    }
    private fun joinSendsToLogin(){
        joinButton.setOnClickListener {
            navigateToLogin()
        }
    }
    private fun joinRoom(roomId: String, enteredCode: String?) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                val roomCode = document.getString("code") ?: ""
                if (!isPublic && roomCode != enteredCode) {
                    toast("Incorrect room code.")
                    return@addOnSuccessListener
                }
                if(!isLoggedIn){
                    toast("Log in")
                    return@addOnSuccessListener
                }
                getUserName(userId) { userName ->
                getUserPhotoUrl(userId) { userUrl ->
                    val participants = getActiveParticipants(document).toMutableMap()
                    if (participants.containsKey(userId)) {  // המשתמש כבר קיים - עדכן את isActive ל- true
                        val updatedParticipant = participants[userId]?.toMutableMap() ?: mutableMapOf()
                        updatedParticipant["isActive"] = true
                        participants[userId] = updatedParticipant
                        db.collection("rooms").document(roomId)
                            .update("participants", participants)
                            .addOnSuccessListener{ updateUserRoomEntry(userId,roomId,true) }
                            .addOnFailureListener{ toast("Failed to reactivate user in room") }
                    } else {
                        val participantData = mapOf(
                            "name" to userName,
                            "role" to "participant",
                            "photoUrl" to userUrl,
                            "isBetting" to false,
                            "joinedOn" to System.currentTimeMillis(),
                            "isActive" to true
                        )
                        addUserToRoom(roomId,userId,participantData){ success ->
                            if(!success){
                                toast("Failed to add user to room")
                                return@addUserToRoom
                            }
                            val betSubject = (document.getString("name") ?: "Unnamed Room")
                            addRoomToUser(userId,roomId,betSubject,isPublic) { success2 ->
                                if(!success2){
                                    toast("Failed to add room to user")
                                    return@addRoomToUser
                                }
                                toast("User added successfully")
                                finish()
                                startActivity(intent)
                            }
                        }
                    }
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
                    var roomsParticipants = getActiveWithBannedParticipants(document)
                    var isRoomCreator = false
                    var hasAlreadyBet = false
                    var isRoomOwner = false
                    var usersBetAmount: Long = 0
                    if (isLoggedIn) {
                        val userParticipant = roomsParticipants[userId]
                        val roomCreatorId = document.getString("createdBy") ?: "None"
                        isRoomCreator = roomCreatorId == userId
                        hasAlreadyBet = userParticipant?.get("isBetting") as? Boolean ?: false
                        val userRole = roomsParticipants[userId]?.get("role") as? String ?: "participant"
                        isRoomOwner = userRole == "owner"
                        usersBetAmount = roomsParticipants[userId]?.get("betAmount") as? Long ?: 0
                    }
                    if(!isRoomOwner){ roomsParticipants = roomsParticipants.filterValues { it["role"]!="banned" } }

                    betType = document.getString("betType") ?: "N/A"

                    roomNameTextView.text = document.getString("name") ?: "N/A"
                    roomTypeTextView.text = betType

                    roomPointsTextView.text = document.getLong("betPoints")?.toString() ?: "0"
                    if(roomPointsTextView.text=="0"){ roomPointsTextView.text = usersBetAmount.toString() }

                    roomDescriptionTextView.text = document.getString("description") ?: "N/A"
                    roomExpirationTextView.text = document.getString("expiration") ?: "N/A"
                    roomIsPublicTextView.text = if(isPublic) "Public" else "Private"

                    val numParticipants = roomsParticipants.size
                    val maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 10
                    participantsCountTextView.text = "$numParticipants/$maxParticipants"

                    if(isRoomCreator){
                        closeBetButton.visibility = View.VISIBLE
                        closeBetButton.setOnClickListener{ showCloseBet() }
                    }
                    if(hasAlreadyBet){ betNowButton.visibility = View.GONE }
                    else{ betNowButton.setOnClickListener{ showBetOptions() } }

                    val participantsViewPager = findViewById<ViewPager2>(R.id.participantsViewPager)
                    val dotsIndicator = findViewById<DotsIndicator>(R.id.participantsDotsIndicator)
                    val adapter = AdapterParticipantsPager(
                        roomsParticipants,
                        isRoomOwner,
                        { clickedUserId -> openUser(clickedUserId) },
                        { targetUserId, newRole -> changeUserRole(targetUserId, newRole) }
                    )
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
        if (pollOptions.isEmpty()) {
            toast("No options available for betting.")
            return
        }
        var selectedOption: String? = null
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose your bet")
        builder.setSingleChoiceItems(pollOptions.toTypedArray(),-1){ _, which ->
            selectedOption = pollOptions[which]
        }
        builder.setPositiveButton("Confirm") { _, _ ->
            if(selectedOption != null){
                if(betType == "Even Bet"){
                    getUserBetPoints(userId){ betPoints ->
                        joinBet(selectedOption!!, betPoints)
                    }
                }
                else if(betType == "Ratio Bet"){ showBetAmountInput(selectedOption!!) }
            }
            else{ toast("Please select an option.") }
        }
        builder.setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }
    private fun showBetAmountInput(selectedOption:String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Bet Amount")
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Confirm") { _, _ ->
            val betAmount = input.text.toString().toLongOrNull() ?: return@setPositiveButton
            joinBet(selectedOption, betAmount)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
    private fun joinBet(selectedOption:String, betPoints:Long) {
        if(!isLoggedIn){
            toast("Log in")
            return
        }
        getUserCurrentPoints(userId){ userPoints ->
            if (userPoints < betPoints) {
                toast("You don't have enough points to join this bet")
                return@getUserCurrentPoints
            }
            val newPoints = userPoints - betPoints
            db.collection("users").document(userId)
                .update("currentPoints", newPoints)
                .addOnSuccessListener {
                    betNowButton.visibility = View.GONE
                    val updates = mapOf(
                        "participants.$userId.isBetting" to true,            // סימון שהמשתמש מהמר
                        "participants.$userId.betOption" to selectedOption,  // שמירת האפשרות שבחר
                        "participants.$userId.betAmount" to betPoints  // שמירת האפשרות שבחר
                    )
                    db.collection("rooms").document(roomId)
                        .update(updates)
                        .addOnSuccessListener { toast("You have joined the bet with option: $selectedOption!") }
                        .addOnFailureListener { e -> toast("Failed to update bet status: ${e.message}") }
                }
                .addOnFailureListener{ toast("Error updating points.") }
        }
    }

    private fun showCloseBet() {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    toast("Room not found")
                    return@addOnSuccessListener
                }
                val roomOwnerId = document.getString("createdBy") ?: "None"
                if (userId != roomOwnerId) {
                    toast("Only the room owner can close the bet")
                    return@addOnSuccessListener
                }
                if (pollOptions.isEmpty()) {
                    toast("No options available for closing the bet.")
                    return@addOnSuccessListener
                }
                var selectedWinningOption: String? = null
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Select the winning option")
                builder.setSingleChoiceItems(pollOptions.toTypedArray(),-1) { _, which ->
                    selectedWinningOption = pollOptions[which]
                }
                builder.setPositiveButton("Confirm") { _, _ ->
                    if(selectedWinningOption != null){
                        if(betType == "Even Bet"){ evenBetResults(document,selectedWinningOption!!) }
                        else if(betType == "Ratio Bet"){ ratioBetResults(document,selectedWinningOption!!) }
                    }
                    else{ toast("Please select a winning option.") }
                }
                builder.setNegativeButton("Cancel", null)
                val dialog = builder.create()
                dialog.show()
            }
            .addOnFailureListener { toast("Error fetching room details.") }
    }
    private fun evenBetResults(document: DocumentSnapshot, winningOption: String) {
        val roomsParticipants = getActiveParticipants(document)
        val winners = roomsParticipants.filter { (_, data) -> data["betOption"] == winningOption }
        val losers = roomsParticipants.filter { (_, data) -> data["betOption"] != winningOption }
        if (losers.isEmpty()) {
            toast("No losers found, cannot distribute winnings.")
            return
        }
        getUserBetPoints(userId){ betPoints ->
            val totalPot = roomsParticipants.size * betPoints
            val rewardPerWinner = totalPot / losers.size
            val batch = db.batch()
            winners.forEach { (userId, _) ->
                val userRef = db.collection("users").document(userId)
                batch.update(userRef, "currentPoints", FieldValue.increment(rewardPerWinner.toDouble()))
            }
            val roomRef = db.collection("rooms").document(roomId)
            batch.update(roomRef,"status","closed") // מעדכן שהחדר נסגר
            batch.commit()
                .addOnSuccessListener {
                    toast("Bet closed. Winners received $rewardPerWinner points.")
                    deleteRoom(roomId) // מוחק את החדר
                }
                .addOnFailureListener{ toast("Failed to process bet results.") }
        }
    }
    private fun ratioBetResults(document: DocumentSnapshot, winningOption: String) {
        val roomsParticipants = getActiveParticipants(document)
        val winners = mutableMapOf<String, Long>()
        var totalPot = 0L

        for ((userId, userData) in roomsParticipants) {
            val betOption = userData["betOption"] as? String ?: continue
            val betAmount = userData["betAmount"] as? Long ?: continue
            totalPot += betAmount
            if (betOption == winningOption) {
                winners[userId] = betAmount
            }
        }

        val totalWinningBet = winners.values.sum()
        if (totalWinningBet > 0) {
            for ((winnerId, winnerBet) in winners) {
                val winnings = (winnerBet * totalPot) / totalWinningBet
                db.collection("users").document(winnerId)
                    .update("currentPoints", FieldValue.increment(winnings))
            }
        }
        toast("Bet closed. Winnings distributed.")
        deleteRoom(roomId) // מוחק את החדר
    }

    private fun initChatView(view: View) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)

        if(userRole == "muted participant" || userStatus == "muted client"){ sendButton.text = "Muted" }
        else {
            sendButton.setOnClickListener {
                val messageText = messageInput.text.toString()
                if (messageText.isNotBlank()) {
                    sendMessage(messageText)
                } else {
                    toast("Message cannot be empty.")
                }
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
        var senderName = "Unknown"
        getUserName(userId){ name -> if(name != null){ senderName = name } }

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

    private fun changeUserRole(userId: String, newRole: String) {
        getParticipantById(userId,roomId) { user ->
            if (user != null) {
                val isBetting = user["isBetting"] as? Boolean ?: false
                val updatedUserRole = user["role"] as? String ?: ""
                if (isBetting && newRole == "banned") {
                    toast("Cannot ban a user who is currently betting.")
                    return@getParticipantById
                }
                val roomUpdates = mutableMapOf<String, Any>("participants.$userId.role" to newRole)
                if(newRole == "banned"){ toggleRoomFromUser(userId, roomId, false) {} }
                else if(updatedUserRole == "banned"){ toggleRoomFromUser(userId,roomId,true){} }
                db.collection("rooms").document(roomId).update(roomUpdates)
                    .addOnSuccessListener {
                        toast("User role updated to $newRole")
                        showRoomDetails()
                    }
                    .addOnFailureListener { e -> toast("Failed to update role: ${e.message}") }
            } else{ toast("User not found in the room.") }
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
        if(!isLoggedIn){
            toast("Log in")
            return
        }
        getParticipantById(userId,roomId){ participantToRemove ->
            if (participantToRemove != null) {
                toggleUserFromRoom(roomId, userId, false) { success ->
                    if (success) {
                        toggleRoomFromUser(userId, roomId, false) { success2 ->
                            if (success2) {   // Now remove the room from the user's rooms array
                                toast("You have left the room")
                                navigateTo(RatingActivity::class.java)
                            } else{ toast("Failed to remove room from user") }
                        }
                    } else{ toast("Failed to remove user from room") }
                }
            } else{ toast("User not found in participants.") }
        }
    }
}

class AdapterParticipantsPager(
    private val participants: Map<String, Map<String, Any>>,
    private val isRoomOwner: Boolean,
    private val onUserClick: (String) -> Unit,
    private val onRoleChange: (String, String) -> Unit
) :
    RecyclerView.Adapter<AdapterParticipantsPager.ParticipantViewHolder>() {
    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.participantName)
        val roleTextView: TextView = itemView.findViewById(R.id.participantRole)
        val profileImageView: ImageView = itemView.findViewById(R.id.participantImage)
        val changeRoleButton: Button = itemView.findViewById(R.id.changeRoleButton)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }
    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val entry = participants.entries.elementAt(position)
        val userId = entry.key
        val participant = entry.value

        holder.nameTextView.text = participant["name"] as? String ?: "Unknown"
        holder.roleTextView.text = participant["role"] as? String ?: "No Role"
        var imageUrl  = participant["photoUrl"]
        Glide.with(holder.profileImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.profileImageView)

        holder.itemView.setOnClickListener { onUserClick(userId) }
        if (isRoomOwner) {
            holder.changeRoleButton.visibility = View.VISIBLE
            holder.changeRoleButton.setOnClickListener {
                showRoleChangeDialog(holder.itemView.context, userId, onRoleChange)
            }
        }
        else { holder.changeRoleButton.visibility = View.GONE }
    }
    override fun getItemCount(): Int = participants.size
    private fun showRoleChangeDialog(context: Context, userId: String, onRoleChange: (String, String) -> Unit) {
        val roles = arrayOf("participant", "warned participant", "muted participant", "banned", "owner")
        AlertDialog.Builder(context)
            .setTitle("Change Role")
            .setItems(roles) { _, which ->
                val selectedRole = roles[which]
                onRoleChange(userId, selectedRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
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