package com.example.bettertogether

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import androidx.work.*
import java.util.concurrent.TimeUnit

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var leaveRoomItem : ImageView             // details

    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseFirestore
    protected lateinit var sharedPreferences: SharedPreferences

    protected lateinit var userId: String
    protected var isLoggedIn: Boolean = false

    companion object {
        // Set DEBUG_MODE to true for debugging (notifications will be checked every 1 minute)
        // Set to false for production (notifications will be checked once per day)
        const val DEBUG_MODE = false
        const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    override fun setContentView(layoutResID: Int) {
        // Inflate the base layout and insert the provided layout into the content frame.
        val baseView = layoutInflater.inflate(R.layout.activity_base, null)
        val contentFrame = baseView.findViewById<FrameLayout>(R.id.activity_content)
        layoutInflater.inflate(layoutResID, contentFrame, true)
        super.setContentView(baseView)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        isLoggedIn = auth.currentUser != null
        userId = auth.currentUser?.uid ?: sharedPreferences.getString("userId",null) ?: ""
        leaveRoomItem = findViewById(R.id.action_leave_room)

        setupBottomNavigation()
        requestNotificationPermissionIfNeeded()
        scheduleBetNotifications() // Schedule bet notifications
    }

    protected fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if (bottomNav == null) return

        val currentActivity = this::class.java
        val selectedItemId = when (currentActivity) {
            HomeActivity::class.java -> R.id.nav_home
            ExplorerActivity::class.java -> R.id.nav_explorer
            NewRoomActivity::class.java -> R.id.nav_add
            RatingActivity::class.java -> R.id.nav_star
            ProfileActivity::class.java -> R.id.nav_profile
            else -> 0
        }
        if (selectedItemId != 0) {
            bottomNav.selectedItemId = selectedItemId
        } else {
            bottomNav.menu.setGroupCheckable(0, true, false)
            for (i in 0 until bottomNav.menu.size()) {
                bottomNav.menu.getItem(i).isChecked = false
            }
        }
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateTo(HomeActivity::class.java)
                R.id.nav_explorer -> navigateTo(ExplorerActivity::class.java)
                R.id.nav_add -> navigateTo(NewRoomActivity::class.java)
                R.id.nav_star -> navigateTo(RatingActivity::class.java)
                R.id.nav_profile -> navigateTo(ProfileActivity::class.java)
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }
    protected fun navigateTo(targetActivity: Class<out AppCompatActivity>) {
        if (this::class.java != targetActivity) {
            val intent = Intent(this, targetActivity)
            startActivity(intent)
            finish()
        }
    }
    protected fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    protected open fun openRoom(roomId: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra("roomId", roomId)
        startActivity(intent)
        finish()
    }
    protected fun openUser(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }
    private fun scheduleBetNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("uid", userId)
            .build()

        if (DEBUG_MODE) {
            val betNotificationWork = OneTimeWorkRequestBuilder<BetNotificationWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(inputData)  // הוספת נתוני קלט
                .build()
            WorkManager.getInstance(this).enqueueUniqueWork(
                "BetNotificationWork",
                ExistingWorkPolicy.REPLACE,
                betNotificationWork
            )
            Log.d("BaseActivity", "Scheduled debug notification work to run in 1 minute.")
        } else {
            val betNotificationWork = PeriodicWorkRequestBuilder<BetNotificationWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInputData(inputData)  // הוספת נתוני קלט
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "BetNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                betNotificationWork
            )
        }
    }

    // --- Helper methods below ---

    protected fun checkUserRole(callback: (String?) -> Unit) {
        if(!isLoggedIn){
            callback(null)
            return
        }
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    callback(role)
                } else { callback(null) }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to check role: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }

    protected fun addRoomToUser(userId: String, roomId: String, betSubject: String, isPublic: Boolean, callback: (Boolean) -> Unit) {
        val userRoomData = hashMapOf(
            "roomId" to roomId,
            "roomName" to betSubject,
            "joinedOn" to System.currentTimeMillis(),
            "isPublic" to isPublic,
            "isActive" to true
        )
        db.collection("users").document(userId)
            .update("rooms", FieldValue.arrayUnion(userRoomData))
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { exception ->
                toast("Error linking room to user: ${exception.message}")
                callback(false)
            }
    }
    protected fun toggleRoomFromUser(userId:String, roomId:String, flag:Boolean, callback:(Boolean) -> Unit) {
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { userDocument ->
            if (!userDocument.exists()) {
                toast("User document not found.")
                callback(false)
                return@addOnSuccessListener
            }
            val rooms = getUserActiveRooms(userDocument)
            if (rooms.isNotEmpty()) {
                val updatedRooms = rooms.map { room ->
                    if(room["roomId"] == roomId){ room.toMutableMap().apply { this["isActive"] = flag } }
                    else{ room }
                }
                userRef.update("rooms", updatedRooms)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { exception -> callback(false) }
                }
            }
            .addOnFailureListener { exception ->
                toast("Error retrieving user data: ${exception.message}")
                callback(false)
            }
    }
    protected fun deleteRoom(roomId: String) {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val participantsMap = getActiveWithBannedParticipants(document)
                    val batch = db.batch()
                    participantsMap.keys.forEach { userId ->
                        toggleRoomFromUser(userId,roomId,false){}
                    }
                    batch.update(roomRef, "isActive", false)
                    batch.commit()
                        .addOnSuccessListener {
                            toast("Room was deactivated.")
                            navigateTo(HomeActivity::class.java)
                        }
                        .addOnFailureListener{ exception -> toast("Error updating users' rooms: ${exception.message}") }
                } else{ toast("Room not found.") }
            }
            .addOnFailureListener{ exception -> toast("Error fetching room: ${exception.message}") }
    }

    protected fun addUserToRoom(roomId: String, userId: String, participantData: Map<String, Comparable<*>?>, callback: (Boolean) -> Unit) {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.get().addOnSuccessListener { document ->
            val updates = if(document.contains("participants")){ mapOf("participants.$userId" to participantData) }
            else { mapOf("participants" to mapOf(userId to participantData)) }

            roomRef.update(updates)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { exception ->
                    toast("Failed to join room: ${exception.message}")
                    callback(false)
                }
        }
    }
    protected fun toggleUserFromRoom(roomId:String, userId:String, flag:Boolean, callback:(Boolean) -> Unit) {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                toast("Room document not found.")
                callback(false)
                return@addOnSuccessListener
            }
            val roomParticipants = getActiveParticipants(document)
            if (!roomParticipants.containsKey(userId)) {
                toast("User not found in room.")
                callback(false)
                return@addOnSuccessListener
            }
            val updatedParticipants = roomParticipants.mapValues { (id, data) ->
                if(id == userId){ data.toMutableMap().apply { this["isActive"] = flag } }
                else { data }
            }
            roomRef.update("participants", updatedParticipants)
                .addOnSuccessListener {
                    if(roomParticipants.size <= 1){ deleteRoom(roomId) }
                    callback(true)
                }
                .addOnFailureListener { exception ->
                    toast("Error updating room data: ${exception.message}")
                    callback(false)
                }
        }.addOnFailureListener { exception ->
            toast("Error retrieving room data: ${exception.message}")
            callback(false)
        }
    }

    protected fun getParticipantById(userId: String, roomId: String, callback: (Map<String, Any>?) -> Unit) {
        val roomRef = db.collection("rooms").document(roomId)

        roomRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val participants = getActiveWithBannedParticipants(document)
                callback(participants[userId])
            } else {
                toast("Room not found.")
                callback(null)
            }
        }.addOnFailureListener { e ->
            toast("Failed to fetch room details: ${e.message}")
            callback(null)
        }
    }

    protected fun getActiveParticipants(roomDoc: DocumentSnapshot): Map<String, Map<String, Any>> {
        val roomsParticipants = getActiveWithBannedParticipants(roomDoc)
        return roomsParticipants.filterValues { it["role"]!="banned" }
    }
    protected fun getActiveWithBannedParticipants(roomDoc: DocumentSnapshot): Map<String, Map<String, Any>> {
        val roomsParticipants = roomDoc.get("participants") as? Map<String, Map<String, Any>> ?: emptyMap()
        return roomsParticipants.filterValues { it["isActive"] == true}
    }
    protected fun getUserActiveRooms(userDoc: DocumentSnapshot): List<Map<String, Any>> {
        val roomIds = userDoc.get("rooms") as? List<Map<String, Any>> ?: emptyList()
        return roomIds.filter { it["isActive"] == true }
    }
    protected fun getUserActiveFriends(userDoc: DocumentSnapshot): Map<String, Map<String, Any>> {
        val friends = userDoc.get("friends") as? Map<String, Map<String, Any>> ?: emptyMap()
        return friends.filterValues { it["isActive"] == true }
    }
    protected fun getUserActiveReceivedRequests(userDoc: DocumentSnapshot): Map<String, Any> {
        val receivedRequests = userDoc.get("receivedRequests") as? Map<String, Any> ?: emptyMap()
        return receivedRequests
    }
    protected fun getUserActiveSentRequests(userDoc: DocumentSnapshot): Map<String, Any> {
        val sentRequests = userDoc.get("sentRequests") as? Map<String, Any> ?: emptyMap()
        return sentRequests
    }

    protected fun getUserStatus(userId: String, callback: (String?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if(document.exists()){ callback(document.getString("role") ?: "Unknown") }
                else { callback(null) }
            }
            .addOnFailureListener { callback(null) }
    }
    protected fun getUserName(userId:String, callback:(String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener{ document ->
                if(document.exists()){ callback(document.getString("displayName") ?: "Unknown") }
                else { callback("Unknown") }
            }
            .addOnFailureListener { callback("Unknown") }
    }
    protected fun getUserPhotoUrl(userId:String, callback:(String?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener{ document ->
                if(document.exists()){ callback(document.getString("photoUrl") ?: "Unknown") }
                else { callback(null) }
            }
            .addOnFailureListener { callback(null) }
    }
    protected fun getUserCurrentPoints(userId:String, callback:(Long) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener{ document ->
                if(document.exists()){ callback(document.getLong("currentPoints") ?: 0) }
                else{ callback(0) }
            }
            .addOnFailureListener { callback(0) }
    }
    protected fun getUserBetPoints(userId:String, callback:(Long) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener{ document ->
                if(document.exists()){ callback(document.getLong("betPoints") ?: 0) }
                else{ callback(0) }
            }
            .addOnFailureListener { callback(0) }
    }

    protected fun showPopularPublicRooms(list: MutableList<DocumentSnapshot>, sliderAdapter: AdapterEvents) {
        db.collection("rooms")
            .whereEqualTo("isEvent", false)
            .whereEqualTo("isPublic", true)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                list.clear()
                list.addAll(
                    querySnapshot.documents.sortedByDescending {
                        val participants = getActiveParticipants(it)
                        participants.size
                    }
                )
                sliderAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                toast("Error fetching rooms: ${exception.message}")
            }
    }
    protected fun loadUserRooms(userId: String, docList: MutableList<DocumentSnapshot>, roomsAdapter: AdapterEvents) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    docList.clear()
                    val roomIds = getUserActiveRooms(document)
                    if (roomIds.isNotEmpty()) {
                        val ids = roomIds.mapNotNull { it["roomId"] as? String }
                        db.collection("rooms")
                            .whereIn(FieldPath.documentId(), ids)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                docList.addAll(querySnapshot.documents)
                                roomsAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { exception -> toast("Error fetching rooms: ${exception.message}") }
                    }
                }
            }
            .addOnFailureListener { exception -> toast("Error fetching user data: ${exception.message}") }
    }
    protected fun loadMyRooms(docList: MutableList<DocumentSnapshot>, roomsAdapter: AdapterEvents) {
        if (!isLoggedIn) {
            toast("Please log in to see your rooms.")
            navigateToLogin()
            return
        }
        loadUserRooms(userId, docList, roomsAdapter)
    }

    protected fun loadImageFromURL(imageUrl: String, imageView: ImageView) {
        if (!isDestroyed && !isFinishing) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.room_placeholder_image)
                .error(R.drawable.room_placeholder_image)
                .into(imageView)
        } else {
            Log.w("BaseActivity", "Attempted to load image for a destroyed or finishing activity")
        }
    }
    protected fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
