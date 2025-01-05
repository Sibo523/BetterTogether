package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FieldValue

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun navigateTo(targetActivity: Class<out AppCompatActivity>) {
        if (this::class.java != targetActivity) {
            val intent = Intent(this, targetActivity)
            startActivity(intent)
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

    protected fun addRoomToUser(userId:String, roomId:String, betSubject:String, role:String, isPublic:Boolean){
        val userRoomData = hashMapOf(
            "roomId" to roomId,
            "roomName" to betSubject,
            "joinedOn" to System.currentTimeMillis(),
            "role" to role,
            "isPublic" to isPublic
        )
        db.collection("users").document(userId)
            .update("rooms", FieldValue.arrayUnion(userRoomData))
            .addOnSuccessListener { recreate() }
            .addOnFailureListener { exception -> toast("Error linking room to user: ${exception.message}") }
    }

    protected fun toast(message : String){
        toast(message)
    }
}
