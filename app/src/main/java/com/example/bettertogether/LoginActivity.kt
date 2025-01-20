package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import java.util.*

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Set background based on the time of day
        if (isDayTime()) {
            updateSubtitleText("Good morning!")
            window.decorView.setBackgroundResource(R.drawable.good_morning_img) // Daytime background
        } else {
            updateSubtitleText("Good night!")
            window.decorView.setBackgroundResource(R.drawable.good_night_img) // Nighttime background
        }

        // Check if the user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkAndCreateUser()
            goToMainScreen()
        } else{ setupGoogleSignIn() }
    }

    private fun isDayTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        return hourOfDay in 6..18 // 6 AM to 6 PM is considered day time
    }

    private fun updateSubtitleText(text: String) {
        val subtitleTextView = findViewById<TextView>(R.id.subtitleText)
        subtitleTextView.text = text
        subtitleTextView.setTextColor(getColor(R.color.white))
    }

    private fun setupGoogleSignIn() {
        val googleSignInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.btnGoogleSignIn)
        googleSignInButton.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)

            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("LoginActivity", "ActivityResult received with code: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginActivity", "Google sign-in successful: ${account.email}")
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e("LoginActivity", "Google sign-in failed", e)
            }
        } else {
            Log.w("LoginActivity", "ActivityResult returned with code: ${result.resultCode}")
        }
    }


    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithCredential:success")
                    checkAndCreateUser()
                    goToMainScreen()
                } else {
                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkAndCreateUser() {
        val user = auth.currentUser
        if (user != null) {
            val userRef = db.collection("users").document(user.uid)
            userRef.get().addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Create a new user document
                    val userData = hashMapOf(
                        "email" to user.email,
                        "displayName" to user.displayName,
                        "createdAt" to System.currentTimeMillis(),
                        "currentPoints" to 100,
                        "rooms" to emptyList<Map<String, Any>>(),
                        "photoUrl" to (user.photoUrl?.toString() ?: "")
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
        }
    }

    private fun goToMainScreen() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
