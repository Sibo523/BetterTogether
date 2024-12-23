package com.example.bettertogether

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreUtils {
    private val db = FirebaseFirestore.getInstance()

    fun isOwnerRole(roomId: String, callback: (Boolean) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            callback(false) // User not logged in
            return
        }

        val userId = currentUser.uid

        db.collection("rooms").document(roomId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val participants = document.get("participants") as? List<Map<String, Any>>
                    if (participants != null) {
                        val userEntry = participants.find { it["id"] == userId }
                        if (userEntry != null && userEntry["role"] == "owner") {
                            callback(true)
                            return@addOnSuccessListener
                        }
                    }
                }
                callback(false)
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreUtils", "Error checking role: ${exception.message}")
                callback(false)
            }
    }
}
