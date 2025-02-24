/**
 * UserProfileActivity displays a user's profile along with their associated rooms and events.
 * It supports various friend management operations (sending, accepting, canceling, and removing friend requests)
 * as well as administrative status changes if the logged-in user is an owner.
 *
 * Functionality Overview:
 *
 * - onCreate(savedInstanceState: Bundle?):
 *     - Initializes UI components for the profile (image, name, points, friendship status, etc.).
 *     - Configures horizontal sliders (RecyclerViews) to display the user's active rooms and event rooms.
 *     - If the current user is an owner, shows a button to change the status of the viewed user.
 *     - Loads the target user's (hisUserId) profile data.
 *
 * - loadUserProfile():
 *     - Retrieves the target user's profile from Firestore using hisUserId.
 *     - Updates UI elements with the user's display name, points, and profile image.
 *     - Loads the user's active rooms and events into their respective sliders.
 *     - Initiates a check of the friendship status if the current user is not viewing their own profile.
 *
 * - checkFriendshipStatus():
 *     - Checks the relationship between the logged-in user and the target user by reading friend lists
 *       and friend request fields from Firestore.
 *     - Updates the UI (friendStatusTextView and actionButton) to reflect the current friendship state.
 *
 * - sendFriendRequest():
 *     - Sends a friend request by updating the "sentRequests" field for the current user and
 *       the "receivedRequests" field for the target user in Firestore.
 *     - Updates the UI to indicate that a friend request has been sent.
 *
 * - acceptFriendRequest():
 *     - Accepts a received friend request by updating the "friends" fields for both users using a Firestore transaction.
 *     - Removes the corresponding friend request entries after successful acceptance.
 *
 * - cancelFriendRequest():
 *     - Cancels a pending friend request by deleting the appropriate fields from both users' documents.
 *     - Updates the UI to reflect the cancellation.
 *
 * - removeFriend():
 *     - Deactivates an existing friendship by setting the friendship's "isActive" flag to false.
 *     - Also clears any existing friend request data between the users.
 *
 * - showStatusChangeDialog():
 *     - Displays an AlertDialog that lets an owner choose a new role for the target user from a predefined list.
 *
 * - changeUserStatus(newRole: String):
 *     - Updates the target user's role in Firestore.
 *     - If the new role is "banned", triggers the removal of the user from all active rooms.
 *
 * - removeUserFromAllRooms(hisUserId: String):
 *     - Retrieves all rooms where the target user is active.
 *     - Iterates through the rooms and toggles the user's active status to false.
 */


package com.example.bettertogether

import android.content.Context
import android.os.Bundle
import android.view.View
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

    private lateinit var hisUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        //initialize views

        profileImageView = findViewById(R.id.profileImageView)
        userNameTextView = findViewById(R.id.userNameTextView)
        userPointsTextView = findViewById(R.id.userPointsTextView)
        friendStatusTextView = findViewById(R.id.friendStatusTextView)
        actionButton = findViewById(R.id.actionButton)

        changeStatusButton = findViewById(R.id.changeStatusButton)

        if(isLoggedIn) {
            getUserStatus(userId) { status ->
                if (status == "owner") {
                    changeStatusButton.visibility = View.VISIBLE
                    changeStatusButton.setOnClickListener { showStatusChangeDialog() }
                }
            }
        }

        hisUserId = intent.getStringExtra("userId") ?: return finish()

        roomsSlider = findViewById(R.id.rooms_slider)
        roomsSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        roomsSliderAdapter = AdapterEvents(roomsList) { room -> openRoom(room.id) }
        roomsSlider.adapter = roomsSliderAdapter

        eventsSlider = findViewById(R.id.events_slider)
        eventsSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        eventsSliderAdapter = AdapterEvents(eventsList) { event -> openRoom(event.id) }
        eventsSlider.adapter = eventsSliderAdapter
        if(userId == hisUserId){
            actionButton.visibility = View.GONE
            friendStatusTextView.visibility = View.GONE
        }
        loadUserProfile()
    }
    //load user profile
    private fun loadUserProfile() {
        //get user info
        db.collection("users").document(hisUserId).get()
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
                    if(userId != hisUserId){
                        checkFriendshipStatus()
                    }
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
    //check friendship status
    private fun checkFriendshipStatus() {
        if(!isLoggedIn){
            actionButton.setOnClickListener { navigateToLogin() }
            return
        }
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val friends = getUserActiveFriends(document)
                val receivedRequests = getUserActiveReceivedRequests(document)
                val sentRequests = getUserActiveSentRequests(document)

                when {
                    friends.containsKey(hisUserId) -> {
                        friendStatusTextView.text = "You are friends"
                        actionButton.text = "Remove Friend"
                        actionButton.setOnClickListener { removeFriend() }
                    }
                    receivedRequests.containsKey(hisUserId) -> {
                        friendStatusTextView.text = "Friend request received"
                        actionButton.text = "Accept Request"
                        actionButton.setOnClickListener { acceptFriendRequest() }
                    }
                    sentRequests.containsKey(hisUserId) -> {
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
    //send friend request
    private fun sendFriendRequest() {

        val userRef = db.collection("users").document(userId)
        val otherUserRef = db.collection("users").document(hisUserId)

        db.runBatch { batch ->
            batch.update(userRef, "sentRequests.$hisUserId", true)
            batch.update(otherUserRef, "receivedRequests.$userId", true)
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

    //accept friend request
    private fun acceptFriendRequest() {
        val userRef = db.collection("users").document(userId)
        val otherUserRef = db.collection("users").document(hisUserId)

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

            transaction.update(userRef, "friends.$hisUserId", newUserFriend)
            transaction.update(otherUserRef, "friends.$userId", newOtherUserFriend)

            transaction.update(userRef, "receivedRequests.$hisUserId", FieldValue.delete())
            transaction.update(otherUserRef, "sentRequests.$userId", FieldValue.delete())
        }.addOnSuccessListener {
            toast("You are now friends!")
            checkFriendshipStatus()
        }.addOnFailureListener { e ->
            toast("Error accepting request: ${e.message}")
        }
    }

    //cancel friend request
    private fun cancelFriendRequest() {
        val userRef = db.collection("users").document(userId)
        val otherUserRef = db.collection("users").document(hisUserId)

        db.runBatch { batch ->
            batch.update(userRef, "sentRequests.$hisUserId", FieldValue.delete())
            batch.update(otherUserRef, "receivedRequests.$userId", FieldValue.delete())
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
    //remove friend
    private fun removeFriend() {
        val userRef = db.collection("users").document(userId)
        val otherUserRef = db.collection("users").document(hisUserId)

        val userUpdate = mapOf("friends.$hisUserId.isActive" to false)
        val otherUserUpdate = mapOf("friends.$userId.isActive" to false)

        db.runBatch { batch ->
            batch.update(userRef, userUpdate)
            batch.update(otherUserRef, otherUserUpdate)

            batch.update(userRef, "sentRequests.$hisUserId", FieldValue.delete())
            batch.update(otherUserRef, "receivedRequests.$userId", FieldValue.delete())
        }.addOnSuccessListener {
            toast("Friend deactivated.")
            checkFriendshipStatus()
        }.addOnFailureListener { e ->
            toast("Error deactivating friend: ${e.message}")
        }
    }
    //show status change dialog
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
    //change user status
    private fun changeUserStatus(newRole: String) {
        val userRef = db.collection("users").document(hisUserId)
        //update user role and remove from all rooms if banned
        userRef.update("role", newRole)
            .addOnSuccessListener {
                toast("User role updated to $newRole in users")
                if (newRole == "banned") {
                    removeUserFromAllRooms(hisUserId)
                }
            }
            .addOnFailureListener { e -> toast("Failed to update role in users: ${e.message}") }
    }
    //remove user from all rooms if banned
    private fun removeUserFromAllRooms(hisUserId: String) {
        db.collection("rooms").whereEqualTo("participants.$hisUserId.isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val roomId = document.id
                    toggleUserFromRoom(roomId, hisUserId, false) { success ->
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
