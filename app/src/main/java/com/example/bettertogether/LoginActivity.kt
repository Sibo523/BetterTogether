package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    // Store our Firebase authentication instance
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Create a launcher for the Google Sign-In activity
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                // Get the signed-in account
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)

                // If successful, authenticate with Firebase
                account?.idToken?.let { token ->
                    firebaseAuthWithGoogle(token)
                }
            } catch (e: ApiException) {
                // Log and show any errors
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Set up Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up the sign-in button click handler
        findViewById<SignInButton>(R.id.btnGoogleSignIn).setOnClickListener {
            startGoogleSignIn()
        }

        // Check if user is already signed in
        checkCurrentUser()
    }

    private fun startGoogleSignIn() {
        // Show loading indicator
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE

        // Start the sign-in flow
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        // Create credentials from the Google ID token
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                // Hide loading indicator
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE

                if (task.isSuccessful) {
                    // Sign in success, go to main screen
                    Log.d(TAG, "signInWithCredential:success")
                    goToMainScreen()
                } else {
                    // Sign in failed, show error
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    val errorMessage = task.exception?.message ?: "Authentication failed"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkCurrentUser() {
        // If user is already signed in, go to main screen
        auth.currentUser?.let {
            goToMainScreen()
        }
    }

    private fun goToMainScreen() {
        // Create intent to go to MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            // Clear the activity stack so user can't go back to login
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}