package com.example.bettertogether

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath

class UserProfileActivity : BaseActivity() {
    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userPointsTextView: TextView
    private lateinit var friendStatusTextView: TextView
    private lateinit var actionButton: Button
    private lateinit var changeStatusButton: Button

    private lateinit var roomsSlider: RecyclerView
    private lateinit var roomsSliderAdapter: AdapterEvents
    private val roomsList = mutableListOf<DocumentSnapshot>()
    private lateinit var eventsSlider: RecyclerView
    private lateinit var eventsSliderAdapter: AdapterEvents
    private val eventsList = mutableListOf<DocumentSnapshot>()

    private lateinit var userId: String
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        profileImageView = findViewById(R.id.profileImageView)
        userNameTextView = findViewById(R.id.userNameTextView)
        userPointsTextView = findViewById(R.id.userPointsTextView)
        friendStatusTextView = findViewById(R.id.friendStatusTextView)
        actionButton = findViewById(R.id.actionButton)
        changeStatusButton = findViewById(R.id.changeStatusButton)
        changeStatusButton.setOnClickListener { showStatusChangeDialog() }

        userId = intent.getStringExtra("userId") ?: return finish()
        currentUserId = auth.currentUser?.uid ?: return finish()

        roomsSlider = findViewById(R.id.rooms_slider)
        roomsSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        roomsSliderAdapter = AdapterEvents(roomsList) { room -> openRoom(room.id) }
        roomsSlider.adapter = roomsSliderAdapter

        eventsSlider = findViewById(R.id.events_slider)
        eventsSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        eventsSliderAdapter = AdapterEvents(eventsList) { event -> openRoom(event.id) }
        eventsSlider.adapter = eventsSliderAdapter

        loadUserProfile()
    }

    private fun loadUserProfile() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("displayName") ?: "Unknown"
                    val userPoints = document.getLong("currentPoints") ?: 0
                    val userImageUrl = document.getString("photoUrl") ?: ""

                    userNameTextView.text = userName
                    userPointsTextView.text = "Points: $userPoints"
                    loadImageFromURL(userImageUrl, profileImageView)

                    val roomIds = getUserActiveRooms(document)
                    if (roomIds.size > 0) {
                        val ids = roomIds.mapNotNull { it["roomId"] as? String }
                        db.collection("rooms")
                            .whereIn(FieldPath.documentId(), ids)
                            .whereEqualTo("isEvent", false)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                roomsList.addAll(querySnapshot.documents)
                                roomsSliderAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener{ exception -> toast("Error fetching rooms: ${exception.message}") }
                        db.collection("rooms")
                            .whereIn(FieldPath.documentId(), ids)
                            .whereEqualTo("isEvent", true)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                eventsList.addAll(querySnapshot.documents)
                                eventsSliderAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener{ exception -> toast("Error fetching rooms: ${exception.message}") }
                    }

                    checkFriendshipStatus()
                } else {
                    toast("User not found.")
                    finish()
                }
            }
            .addOnFailureListener {
                toast("Failed to load user.")
                finish()
            }
    }

    private fun checkFriendshipStatus() {
        val userRef = db.collection("users").document(currentUserId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val friends = getUserActiveFriends(document)
                val receivedRequests = getUserActiveReceivedRequests(document)
                val sentRequests = getUserActiveSentRequests(document)

                when {
                    friends.containsKey(userId) -> {
                        friendStatusTextView.text = "You are friends"
                        actionButton.text = "Remove Friend"
                        actionButton.setOnClickListener { removeFriend() }
                    }
                    receivedRequests.containsKey(userId) -> {
                        friendStatusTextView.text = "Friend request received"
                        actionButton.text = "Accept Request"
                        actionButton.setOnClickListener { acceptFriendRequest() }
                    }
                    sentRequests.containsKey(userId) -> {
                        friendStatusTextView.text = "Friend request sent"
                        actionButton.text = "Cancel Request"
                        actionButton.isEnabled = true
                        actionButton.setOnClickListener { cancelFriendRequest() }
                    }
                    else -> {
                        friendStatusTextView.text = "Not Friends"
                        actionButton.text = "Send Friend Request"
                        actionButton.setOnClickListener { sendFriendRequest() }
                    }
                }
            }
        }
    }

    private fun sendFriendRequest() {
        val userRef = db.collection("users").document(currentUserId)
        val otherUserRef = db.collection("users").document(userId)

        db.runBatch { batch ->
            batch.update(userRef, "sentRequests.$userId", true)
            batch.update(otherUserRef, "receivedRequests.$currentUserId", true)
        }.addOnSuccessListener {
            toast("Friend request sent!")

            friendStatusTextView.text = "Friend request sent"
            actionButton.text = "Cancel Request"
            actionButton.isEnabled = true
            actionButton.setOnClickListener { cancelFriendRequest() }
        }.addOnFailureListener { e ->
            toast("Error sending request: ${e.message}")
        }
    }
    private fun acceptFriendRequest() {
        val userRef = db.collection("users").document(currentUserId)
        val otherUserRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            val otherUserDoc = transaction.get(otherUserRef)

            val newUserFriend = mapOf(
                "name" to (otherUserDoc.getString("displayName") ?: ""),
                "isActive" to true
            )
            val newOtherUserFriend = mapOf(
                "name" to (userDoc.getString("displayName") ?: ""),
                "isActive" to true
            )

            transaction.update(userRef, "friends.$userId", newUserFriend)
            transaction.update(otherUserRef, "friends.$currentUserId", newOtherUserFriend)

            transaction.update(userRef, "receivedRequests.$userId", FieldValue.delete())
            transaction.update(otherUserRef, "sentRequests.$currentUserId", FieldValue.delete())
        }.addOnSuccessListener {
            toast("You are now friends!")
            checkFriendshipStatus()
        }.addOnFailureListener { e ->
            toast("Error accepting request: ${e.message}")
        }
    }
    private fun cancelFriendRequest() {
        val userRef = db.collection("users").document(currentUserId)
        val otherUserRef = db.collection("users").document(userId)

        db.runBatch { batch ->
            batch.update(userRef, "sentRequests.$userId", FieldValue.delete())
            batch.update(otherUserRef, "receivedRequests.$currentUserId", FieldValue.delete())
        }.addOnSuccessListener {
            toast("Friend request canceled!")

            friendStatusTextView.text = "Not Friends"
            actionButton.text = "Send Friend Request"
            actionButton.isEnabled = true
            actionButton.setOnClickListener { sendFriendRequest() }
        }.addOnFailureListener { e ->
            toast("Error canceling request: ${e.message}")
        }
    }
    private fun removeFriend() {
        val userRef = db.collection("users").document(currentUserId)
        val otherUserRef = db.collection("users").document(userId)

        val userUpdate = mapOf("friends.$userId.isActive" to false)
        val otherUserUpdate = mapOf("friends.$currentUserId.isActive" to false)

        db.runBatch { batch ->
            batch.update(userRef, userUpdate)
            batch.update(otherUserRef, otherUserUpdate)

            batch.update(userRef, "sentRequests.$userId", FieldValue.delete())
            batch.update(otherUserRef, "receivedRequests.$currentUserId", FieldValue.delete())
        }.addOnSuccessListener {
            toast("Friend deactivated.")
            checkFriendshipStatus()
        }.addOnFailureListener { e ->
            toast("Error deactivating friend: ${e.message}")
        }
    }

    private fun showStatusChangeDialog() {
        val roles = arrayOf("client", "warned client", "muted client", "banned", "owner")
        AlertDialog.Builder(this)
            .setTitle("Change Status")
            .setItems(roles) { _, which ->
                val selectedRole = roles[which]
                changeUserStatus(selectedRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun changeUserStatus(newRole: String) {
        val userRef = db.collection("users").document(userId)

        userRef.update("role", newRole)
            .addOnSuccessListener {
                toast("User role updated to $newRole in users")
                if (newRole == "banned") {
                    removeUserFromAllRooms(userId)
                }
            }
            .addOnFailureListener { e -> toast("Failed to update role in users: ${e.message}") }
    }
    private fun removeUserFromAllRooms(userId: String) {
        db.collection("rooms").whereEqualTo("participants.$userId.isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val roomId = document.id
                    toggleUserFromRoom(roomId, userId, false) { success ->
                        if(success){ toast("User removed from room $roomId") }
                        else{ toast("Failed to remove user from room $roomId") }
                    }
                }
            }
            .addOnFailureListener { e ->
                toast("Error fetching user's rooms: ${e.message}")
            }
    }

}
