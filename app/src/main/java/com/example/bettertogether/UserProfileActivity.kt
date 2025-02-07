package com.example.bettertogether

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.firestore.FieldValue

class UserProfileActivity : BaseActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userPointsTextView: TextView
    private lateinit var friendStatusTextView: TextView
    private lateinit var actionButton: Button

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

        userId = intent.getStringExtra("userId") ?: return finish()
        currentUserId = auth.currentUser?.uid ?: return finish()

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
}
