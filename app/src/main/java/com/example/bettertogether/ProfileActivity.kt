package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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

        setupBottomNavigation()
    }

    private fun displayProfile() {
        val user: FirebaseUser? = auth.currentUser

        if (user != null) {
            // Set name and email from FirebaseAuth
            nameTextView.text = user.displayName ?: "N/A"
            emailTextView.text = user.email ?: "N/A"

            // Fetch additional user data from Firestore
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
                        Toast.makeText(this, "No additional user data found.", Toast.LENGTH_SHORT).show()
                        // Optionally, set default values
                        usernameTextView.text = "N/A"
                        genderTextView.text = "N/A"
                        ageTextView.text = "N/A"
                        dobTextView.text = "N/A"
                        mobileTextView.text = "N/A"
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show()
                    // Optionally, set default values
                    usernameTextView.text = "N/A"
                    genderTextView.text = "N/A"
                    ageTextView.text = "N/A"
                    dobTextView.text = "N/A"
                    mobileTextView.text = "N/A"
                }

            // Load profile picture
            val photoUrl = user.photoUrl
            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile) // Optional fallback image
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile)
            }
        } else {
            // Handle the case where the user is not logged in
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            // Redirect to login if necessary
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    private fun signOut() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show()
    }
}
