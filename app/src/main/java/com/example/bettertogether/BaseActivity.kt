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
                    navigateTo(RoomsActivity::class.java)
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
    protected fun openRoom(roomId: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra("roomId", roomId)
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
            "isPublic" to isPublic
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
                val rooms = userDocument.get("rooms") as? List<Map<String, Any>> ?: emptyList()
                val roomToRemove = rooms.find { it["roomId"] == roomId }
                if (roomToRemove == null) {
                    toast("Room not found in user data.")
                    callback(false)
                    return@addOnSuccessListener
                }
                db.collection("users").document(userId)
                    .update("rooms", FieldValue.arrayRemove(roomToRemove))
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
        db.collection("rooms").document(roomId).delete()
            .addOnSuccessListener { toast("Room was deleted.") }
            .addOnFailureListener { exception -> toast("Error deleting empty room: ${exception.message}") }
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
            if (!document.exists() || !document.contains("participants.$userId")) {
                toast("User not found in room.")
                callback(false)
                return@addOnSuccessListener
            }
            roomRef.update("participants.$userId", FieldValue.delete())
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { exception ->
                    toast("Error removing user from room: ${exception.message}")
                    callback(false)
                }
        }
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
                .placeholder(R.drawable.ic_profile) // תמונה שתוצג בזמן הטעינה (אופציונלי).
                .error(R.drawable.ic_profile) // תמונה שתוצג אם הטעינה נכשלה (אופציונלי).
                .into(imageView)
        } else {
            Log.w("BaseActivity", "Attempted to load image for a destroyed or finishing activity")
        }
    }

    protected fun toast(message : String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
