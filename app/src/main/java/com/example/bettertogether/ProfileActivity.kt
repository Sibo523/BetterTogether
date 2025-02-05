package com.example.bettertogether

import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser

class ProfileActivity : BaseActivity() {

    // Declare UI components
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var genderTextView: TextView
    private lateinit var ageTextView: TextView
    private lateinit var dobTextView: TextView
    private lateinit var mobileTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var signOutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile) // Make sure this matches your XML file name

        // Initialize UI components
        profileImageView = findViewById(R.id.profile_image)
        nameTextView = findViewById(R.id.profile_name)
        bioTextView = findViewById(R.id.profile_bio)
        emailTextView = findViewById(R.id.profile_email_value)
        usernameTextView = findViewById(R.id.profile_username_value)
        genderTextView = findViewById(R.id.profile_gender_value)
        ageTextView = findViewById(R.id.profile_age_value)
        dobTextView = findViewById(R.id.profile_dob_value)
        mobileTextView = findViewById(R.id.profile_mobile_value)
        signOutButton = findViewById(R.id.sign_out_button)

        // 1) Check if we got "USER_ID_KEY" from an Intent
        val otherUserId = intent.getStringExtra("USER_ID_KEY")

        if (!otherUserId.isNullOrBlank()) {
            // If we have another user's ID, load *their* profile from Firestore
            loadUserProfile(otherUserId)

            // Optionally hide the signOutButton if you're viewing someone else's profile
            // signOutButton.visibility = View.GONE
        } else {
            // 2) Otherwise, load the current user's profile
            loadCurrentUserProfile()
        }

        // Sign out when button is clicked (only relevant for the currently logged-in user)
        signOutButton.setOnClickListener {
            signOut()
        }
    }

    /**
     * Loads the currently logged-in user's profile information.
     */
    private fun loadCurrentUserProfile() {
        val user: FirebaseUser? = auth.currentUser
        if (user == null) {
            toast("Please log in to see your profile.")
            navigateToLogin()
            return
        }

        // Basic FirebaseAuth info
        nameTextView.text = user.displayName ?: "N/A"
        emailTextView.text = user.email ?: "N/A"

        // Then fetch additional Firestore fields
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    usernameTextView.text = document.getString("username") ?: "N/A"
                    genderTextView.text = document.getString("gender") ?: "N/A"
                    ageTextView.text = document.getLong("age")?.toString() ?: "N/A"
                    dobTextView.text = document.getString("dob") ?: "N/A"
                    mobileTextView.text = document.getString("mobile") ?: "N/A"
                    bioTextView.text = document.getString("bio") ?: ""

                    val photoUrl = document.getString("photoUrl") ?: ""
                    loadUserPhoto(photoUrl)
                } else {
                    toast("No additional user data found for your profile.")
                }
            }
            .addOnFailureListener { exception ->
                toast("Failed to fetch your user data: ${exception.message}")
            }
    }

    /**
     * Loads *another* user's profile info from Firestore by their user ID (UID).
     */
    private fun loadUserProfile(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Fill the UI with that user's data
                    val displayName = document.getString("displayName") ?: "N/A"
                    nameTextView.text = displayName

                    val email = document.getString("email") ?: "N/A"
                    emailTextView.text = email

                    usernameTextView.text = document.getString("username") ?: "N/A"
                    genderTextView.text = document.getString("gender") ?: "N/A"
                    ageTextView.text = document.getLong("age")?.toString() ?: "N/A"
                    dobTextView.text = document.getString("dob") ?: "N/A"
                    mobileTextView.text = document.getString("mobile") ?: "N/A"
                    bioTextView.text = document.getString("bio") ?: ""

                    val photoUrl = document.getString("photoUrl") ?: ""
                    loadUserPhoto(photoUrl)
                } else {
                    toast("No user found with ID: $userId")
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                toast("Failed to fetch user data: ${exception.message}")
                finish()
            }
    }

    /**
     * Helper function to load the photo with Glide.
     */
    private fun loadUserPhoto(photoUrl: String) {
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_profile) // Fallback placeholder
            .error(R.drawable.ic_profile)       // If the URL fails
            .into(profileImageView)
    }

    /**
     * Sign out from Firebase and Google, then go to login screen.
     */
    private fun signOut() {
        auth.signOut()

        // Revoke the Google Sign-In session if using Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                toast("Signed out successfully!")
                navigateToLogin()
            }
        }
    }
}
