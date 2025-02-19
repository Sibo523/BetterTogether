package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

            // אם המשתמש מנותק, נוודא שהוא באמת מנותק לפני התחברות
            auth.signOut()

            val googleSignInClient = GoogleSignIn.getClient(
                this,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            )

            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }
    }

    // תוצאה מחלון Google Sign-In
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.e("LoginActivityLog", "Google sign-in failed", e)
                    toast("Authentication failed: ${e.message}")
                }
            }
        }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivityLog", "Google sign-in successful: ${auth.currentUser?.email}")
                    checkAndCreateUser()
                    goToMainScreen()
                } else {
                    Log.e("LoginActivityLog", "Google authentication failed", task.exception)
                    toast("Authentication failed: ${task.exception?.message}")
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
                        "role" to "client",
                        "lastLoginDate" to "2024-02-18",
                        "loginStreak" to 3
                    )
                    userRef.set(userData)
                        .addOnSuccessListener{ Log.d("Firestore","User document created successfully") }
                        .addOnFailureListener{ exception -> Log.e("Firestore","Error creating user document",exception) }
                } else{ Log.d("Firestore", "User document already exists") }
            }.addOnFailureListener{ exception -> Log.e("Firestore","Error checking user document",exception) }
        } else{ Log.w("LoginActivity", "No user authenticated, cannot check or create Firestore document") }
    }

    private fun goToMainScreen() {
        Log.d("LoginActivityLog", "Navigating to HomeActivity")
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
