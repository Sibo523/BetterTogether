package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get the current user and update the UI
        auth.currentUser?.let { user ->
            // Show welcome message with user's name
            findViewById<TextView>(R.id.welcomeText).text = "Welcome, ${user.displayName}"
            // Show user's email
            findViewById<TextView>(R.id.userEmail).text = user.email
        }

        // Set up sign out button
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            signOut()
        }
    }

    private fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Set up Google Sign-In client for signing out
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        // Sign out from Google
        val signInClient = GoogleSignIn.getClient(this, gso)
        signInClient.signOut().addOnCompleteListener {
            // Go back to login screen
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}