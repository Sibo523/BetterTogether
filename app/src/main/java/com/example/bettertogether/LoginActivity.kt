package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import java.util.*

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LoginActivityLog", "onCreate called")
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Set background based on the time of day
        if (isDayTime()) {
            updateSubtitleText("Good morning!")
            window.decorView.setBackgroundResource(R.drawable.good_morning_img) // Daytime background
            Log.d("LoginActivityLog", "Set daytime background")
        } else {
            updateSubtitleText("Good night!")
            window.decorView.setBackgroundResource(R.drawable.good_night_img) // Nighttime background
            Log.d("LoginActivityLog", "Set nighttime background")
        }

        // Check if the user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("LoginActivityLog", "User already signed in: ${currentUser.email}")
            checkAndCreateUser()
            goToMainScreen()
        } else {
            Log.d("LoginActivityLog", "No user signed in, setting up Google Sign-In")
            setupGoogleSignIn()
        }
    }

    private fun isDayTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        Log.d("LoginActivityLog", "Current hour of day: $hourOfDay")
        return hourOfDay in 6..18 // 6 AM to 6 PM is considered day time
    }

    private fun updateSubtitleText(text: String) {
        val subtitleTextView = findViewById<TextView>(R.id.subtitleText)
        subtitleTextView.text = text
        subtitleTextView.setTextColor(getColor(R.color.white))
        Log.d("LoginActivityLog", "Subtitle updated to: $text")
    }

    private fun setupGoogleSignIn() {
        val googleSignInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.btnGoogleSignIn)
        googleSignInButton.setOnClickListener {
            Log.d("LoginActivityLog", "Google Sign-In button clicked")

            val provider = OAuthProvider.newBuilder("google.com")

            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener { authResult ->
                    Log.d("LoginActivityLog", "Google sign-in successful: ${authResult.user?.email}")
                    checkAndCreateUser()
                    goToMainScreen()
                }
                .addOnFailureListener { e ->
                    Log.e("LoginActivityLog", "Google sign-in failed", e)
                    toast("Authentication failed: ${e.message}")
                }
        }
    }

    private fun checkAndCreateUser() {
        val user = auth.currentUser
        if (user != null) {
            Log.d("LoginActivityLog", "Checking user in Firestore: ${user.uid}")
            val userRef = db.collection("users").document(user.uid)
            userRef.get().addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.d("LoginActivityLog", "User not found in Firestore, creating new document")
                    val userData = hashMapOf(
                        "email" to user.email,
                        "displayName" to user.displayName,
                        "createdAt" to System.currentTimeMillis(),
                        "currentPoints" to 1000,
                        "rooms" to emptyList<Map<String, Any>>(),
                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                        "role" to "client"
                    )
                    userRef.set(userData)
                        .addOnSuccessListener {
                            Log.d("Firestore", "User document created successfully")
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error creating user document", exception)
                        }
                } else {
                    Log.d("Firestore", "User document already exists")
                }
            }.addOnFailureListener { exception ->
                Log.e("Firestore", "Error checking user document", exception)
            }
        } else {
            Log.w("LoginActivity", "No user authenticated, cannot check or create Firestore document")
        }
    }

    private fun goToMainScreen() {
        Log.d("LoginActivityLog", "Navigating to HomeActivity")
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
