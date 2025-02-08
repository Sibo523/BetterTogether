package com.example.bettertogether

import android.content.Intent
import android.util.Log
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.QuerySnapshot

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseFirestore

    override fun setContentView(layoutResID: Int) {
        val baseView = layoutInflater.inflate(R.layout.activity_base, null)
        val contentFrame = baseView.findViewById<FrameLayout>(R.id.activity_content)
        layoutInflater.inflate(layoutResID, contentFrame, true)
        super.setContentView(baseView)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        setupBottomNavigation()
    }
    protected fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if(bottomNav == null){ return }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.nav_explorer -> {
                    navigateTo(ExplorerActivity::class.java)
                    true
                }
                R.id.nav_add -> {
                    navigateTo(NewRoomActivity::class.java)
                    true
                }
                R.id.nav_star -> {
                    navigateTo(RatingActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    navigateTo(ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    protected fun navigateToLogin(){
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    protected fun navigateTo(targetActivity: Class<out AppCompatActivity>) {
        if (this::class.java != targetActivity) {
            val intent = Intent(this, targetActivity)
            startActivity(intent)
            finish()
        }
    }
    protected open fun openRoom(roomId: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra("roomId", roomId)
        startActivity(intent)
    }
    protected fun openUser(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    protected fun checkUserRole(callback: (String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If the user is not logged in, return null
            callback(null)
            return
        }
        val userId = currentUser.uid
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    callback(role) // Return the role to the caller
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to check role: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }

    protected fun addRoomToUser(userId: String, roomId: String, betSubject: String, role: String, isPublic: Boolean, callback: (Boolean) -> Unit) {
        val userRoomData = hashMapOf(
            "roomId" to roomId,
            "roomName" to betSubject,
            "joinedOn" to System.currentTimeMillis(),
            "role" to role,
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
    protected fun removeRoomFromUser(userId: String, roomId: String, callback: (Boolean) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDocument ->
                if (!userDocument.exists()) {
                    toast("User document not found.")
                    callback(false)
                    return@addOnSuccessListener
                }
                val rooms = getUserActiveRooms(userDocument)
                val updatedRooms = rooms.map { room ->
                    if(room["roomId"] == roomId){ room.toMutableMap().apply { this["isActive"] = false } }
                    else{ room }
                }
                db.collection("users").document(userId)
                    .update("rooms", updatedRooms)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { exception ->
                        toast("Error updating user data: ${exception.message}")
                        callback(false)
                    }
            }
            .addOnFailureListener { exception ->
                toast("Error retrieving user data: ${exception.message}")
                callback(false)
            }
    }
    protected fun deleteRoom(roomId:String){
        db.collection("rooms").document(roomId)
            .update("isActive", false)
            .addOnSuccessListener { toast("Room was deactivated.") }
            .addOnFailureListener { exception -> toast("Error deactivating room: ${exception.message}") }
        navigateTo(HomeActivity::class.java)
    }
    protected fun addUserToRoom(roomId: String, userId: String, participantData: Map<String, Comparable<*>?>, callback: (Boolean) -> Unit) {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.get().addOnSuccessListener { document ->
            val updates = if (document.contains("participants")) {
                mapOf("participants.$userId" to participantData)
            } else {
                mapOf("participants" to mapOf(userId to participantData)) // יצירת מפתח participants
            }

            roomRef.update(updates)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { exception ->
                    toast("Failed to join room: ${exception.message}")
                    callback(false)
                }
        }
    }
    protected fun removeUserFromRoom(roomId: String, userId: String, callback: (Boolean) -> Unit) {
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
                if(id == userId){ data.toMutableMap().apply { this["isActive"] = false } }
                else{ data }
            }
            roomRef.update("participants", updatedParticipants)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { exception ->
                    toast("Error updating room data: ${exception.message}")
                    callback(false)
                }
        }.addOnFailureListener { exception ->
            toast("Error retrieving room data: ${exception.message}")
            callback(false)
        }
    }

    protected fun getActiveParticipants(roomDoc: DocumentSnapshot): Map<String, Map<String, Any>> {
        val roomsParticipants = roomDoc.get("participants") as? Map<String, Map<String, Any>> ?: emptyMap()
        return roomsParticipants.filterValues { it["isActive"] == true }
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

    protected fun showPopularPublicRooms(list:MutableList<Map<String,Any>>, sliderAdapter:AdapterPopularRooms){
        db.collection("rooms")
            .whereEqualTo("isEvent", false)
            .whereEqualTo("isPublic", true)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                list.clear()
                list.addAll(querySnapshot.documents.map { document ->
                    val roomsParticipants = getActiveParticipants(document)
                    val participantsCount = roomsParticipants.size
                    val maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 10

                    mapOf(
                        "id" to document.id,
                        "name" to (document.getString("name") ?: "Unnamed Room"),
                        "participantsCount" to participantsCount,
                        "maxParticipants" to maxParticipants,
                        "participants" to roomsParticipants
                    )
                }.sortedByDescending { it["participantsCount"] as? Int ?: 0 })
                sliderAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception -> toast("Error fetching rooms: ${exception.message}") }
    }
    protected fun loadUserRooms(userId:String, docList:MutableList<DocumentSnapshot>, roomsAdapter:AdapterEvents){
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    docList.clear()
                    val roomIds = getUserActiveRooms(document)
                    if (roomIds.size > 0) {
                        val ids = roomIds.mapNotNull { it["roomId"] as? String }
                        db.collection("rooms")
                            .whereIn(FieldPath.documentId(), ids)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                docList.addAll(querySnapshot.documents)
                                roomsAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener{ exception -> toast("Error fetching rooms: ${exception.message}") }
                    }
                }
            }
            .addOnFailureListener{ exception -> toast("Error fetching user data: ${exception.message}") }
    }
    protected fun loadMyRooms(docList:MutableList<DocumentSnapshot>, roomsAdapter:AdapterEvents) {
        val user = auth.currentUser
        if(user == null){
            toast("Please log in to see your rooms.")
            navigateToLogin()
            return
        }
        loadUserRooms(user.uid,docList,roomsAdapter)
    }

    protected fun loadUserPhoto(imageView:ImageView){
        val user = auth.currentUser
        if (user == null) { return }
        val photoUrl = user.photoUrl
        loadImageFromURL(photoUrl.toString(),imageView)
    }
    protected fun loadImageFromURL(imageUrl: String, imageView: ImageView) {
        if (!isDestroyed && !isFinishing) {
            Glide.with(this) // `this` הוא הקונטקסט, אם אתה נמצא בתוך Activity או Fragment.
                .load(imageUrl) // כתובת ה-URL של התמונה.
                .placeholder(R.drawable.room_placeholder_image) // תמונה שתוצג בזמן הטעינה (אופציונלי).
                .error(R.drawable.room_placeholder_image) // תמונה שתוצג אם הטעינה נכשלה (אופציונלי).
                .into(imageView)
        } else {
            Log.w("BaseActivity", "Attempted to load image for a destroyed or finishing activity")
        }
    }

    protected fun toast(message : String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
