package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

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
    private lateinit var signOutButton: Button
    private lateinit var bioTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile) // Ensure this matches your XML file name

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

        displayProfile()

        signOutButton.setOnClickListener {
            signOut()
        }
    }

    private fun displayProfile() {
        val user: FirebaseUser? = auth.currentUser
        if (user == null) {
            toast("Please log in to see your profile.")
            navigateToLogin()
        } else{    // Set name and email from FirebaseAuth
            nameTextView.text = user.displayName ?: "N/A"
            emailTextView.text = user.email ?: "N/A"
            val userId = user.uid
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        usernameTextView.text = document.getString("username") ?: "N/A"
                        genderTextView.text = document.getString("gender") ?: "N/A"
                        ageTextView.text = document.getLong("age")?.toString() ?: "N/A"
                        dobTextView.text = document.getString("dob") ?: "N/A"
                        mobileTextView.text = document.getString("mobile") ?: "N/A"
                    } else {
                        toast("No additional user data found.")
                        // Optionally, set default values
                        usernameTextView.text = "N/A"
                        genderTextView.text = "N/A"
                        ageTextView.text = "N/A"
                        dobTextView.text = "N/A"
                        mobileTextView.text = "N/A"
                    }
                }
                .addOnFailureListener { exception ->
                    toast("Failed to fetch user data.")
                    // Optionally, set default values
                    usernameTextView.text = "N/A"
                    genderTextView.text = "N/A"
                    ageTextView.text = "N/A"
                    dobTextView.text = "N/A"
                    mobileTextView.text = "N/A"
                }
            loadUserPhoto(profileImageView)
        }
    }

    private fun signOut() {            // Sign out from Firebase
        auth.signOut()
        // Revoke the Google Sign-In session
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
