package com.example.bettertogether

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : BaseActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var fullNameInput: EditText
    private lateinit var txtDontHaveAccount: TextView
    private lateinit var txtAlreadyHaveAccount: TextView
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: com.google.android.gms.common.SignInButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // אם המשתמש מחובר, נעביר אותו ישירות למסך הראשי
        val currentUser = auth.currentUser
        if (currentUser != null) {
            saveUserLocally(currentUser)
            goToMainScreen()
            return
        }
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
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        fullNameInput = findViewById(R.id.full_name_input)
        loginButton = findViewById(R.id.btnLogin)
        googleSignInButton = findViewById(R.id.btnGoogleSignIn)
        txtDontHaveAccount = findViewById(R.id.txtDontHaveAccount)
        txtAlreadyHaveAccount = findViewById(R.id.txtAlreadyHaveAccount)

        loginButton.setOnClickListener { loginWithEmail() }
        txtDontHaveAccount.setOnClickListener{
            fullNameInput.visibility = View.VISIBLE
            confirmPasswordInput.visibility = View.VISIBLE
            txtDontHaveAccount.visibility = View.GONE
            txtAlreadyHaveAccount.visibility = View.VISIBLE
            loginButton.setOnClickListener { registerWithEmail() }
        }
        txtAlreadyHaveAccount.setOnClickListener{
            fullNameInput.visibility = View.GONE
            confirmPasswordInput.visibility = View.GONE
            txtDontHaveAccount.visibility = View.VISIBLE
            txtAlreadyHaveAccount.visibility = View.GONE
            loginButton.setOnClickListener { loginWithEmail() }
        }
        googleSignInButton.setOnClickListener { setupGoogleSignIn() }
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
        auth.signOut() // לוודא שהמשתמש לא מחובר לפני ההתחברות
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
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                toast("Google sign-in failed: ${e.message}")
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserLocally(user!!)
                    checkAndCreateUser(user)
                    goToMainScreen()
                } else {
                    toast("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun loginWithEmail() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            toast("Please fill in all fields")
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserLocally(user!!)
                    goToMainScreen()
                } else{ toast("User not found?") }
            }
    }
    private fun registerWithEmail() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()
        val fullName = fullNameInput.text.toString().trim()
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || fullName.isEmpty()) {
            toast("Please fill in all fields")
            return
        }
        if (password != confirmPassword) {
            toast("Passwords do not match")
            return
        }
        if (password.length < 6) {
            toast("Password must be at least 6 characters long")
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val profileUpdates = userProfileChangeRequest { displayName = fullName }
                        user.updateProfile(profileUpdates).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                saveUserLocally(user)   // ✅ שומר את השם הנכון מקומית
                                checkAndCreateUser(user)  // ✅ שומר את המשתמש ב-Firestore עם השם החדש
                                goToMainScreen()
                            } else{ toast("Error updating profile: ${updateTask.exception?.message}") }
                        }
                    }
                } else { toast("Registration failed: ${task.exception?.message}") }
            }
    }

    private fun checkAndCreateUser(user: FirebaseUser) {
        val userRef = db.collection("users").document(user.uid)
        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userData = hashMapOf(
                    "email" to user.email,
                    "displayName" to (user.displayName ?: "User"),
                    "createdAt" to System.currentTimeMillis(),
                    "currentPoints" to 1000,
                    "rooms" to emptyList<Map<String, Any>>(),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "role" to "client",
                    "lastLoginDate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    "loginStreak" to 0
                )
                userRef.set(userData)
            }
        }
    }
    private fun saveUserLocally(user: FirebaseUser) {
        val editor = sharedPreferences.edit()
        editor.putString("userId", user.uid)
        editor.putString("userEmail", user.email)
        editor.putString("userName", user.displayName ?: "User")
        editor.apply()
    }

    private fun goToMainScreen() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
