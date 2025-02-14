package com.example.bettertogether

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileActivity : BaseActivity() {

    // UI components for image
    private lateinit var profileImageView: ImageView
    private lateinit var editImageButton: ImageView
    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView

    // UI components for profile data
    private lateinit var editDataButton: Button
    private lateinit var profileBioEditText: EditText
    private lateinit var profileUsernameEditText: EditText
    private lateinit var profileGenderEditText: EditText
    private lateinit var profileAgeEditText: EditText
    private lateinit var profileDobEditText: EditText
    private lateinit var profileMobileEditText: EditText

    // Imgur upload request code
    private val IMGUR_REQUEST_CODE = 101
    var uploadedImageUrl: String? = null

    // Flag to track edit mode for profile data
    private var isDataEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize image UI components
        profileImageView = findViewById(R.id.profile_image)
        editImageButton = findViewById(R.id.edit_image_button)
        profileNameTextView = findViewById(R.id.profile_name)
        profileEmailTextView = findViewById(R.id.profile_email_value)

        // Initialize profile data UI components
        editDataButton = findViewById(R.id.edit_data_button)
        profileBioEditText = findViewById(R.id.profile_bio)
        profileUsernameEditText = findViewById(R.id.profile_username_value)
        profileGenderEditText = findViewById(R.id.profile_gender_value)
        profileAgeEditText = findViewById(R.id.profile_age_value)
        profileDobEditText = findViewById(R.id.profile_dob_value)
        profileMobileEditText = findViewById(R.id.profile_mobile_value)

        // Set click listener on image edit icon to open gallery for image selection
        editImageButton.setOnClickListener { openGallery() }

        // Set click listener on profile data edit button to toggle edit mode for text fields
        editDataButton.setOnClickListener {
            if (!isDataEditMode) {
                isDataEditMode = true
                setDataFieldsEnabled(true)
                editDataButton.text = "Save Profile Data"
            } else {
                saveProfileData()
            }
        }

        // Load profile information:
        // If a "USER_ID_KEY" extra is provided, load that user's profile; otherwise load current user's profile.
        val otherUserId = intent.getStringExtra("USER_ID_KEY")
        if (!otherUserId.isNullOrBlank()) {
            loadUserProfile(otherUserId)
            // Hide data edit button if viewing another user's profile
            editDataButton.visibility = android.view.View.GONE
        } else {
            loadCurrentUserProfile()
        }

        // Set sign out button listener
        findViewById<Button>(R.id.sign_out_button).setOnClickListener { signOut() }
    }

    /**
     * Enables or disables editing for profile data text fields.
     */
    private fun setDataFieldsEnabled(enabled: Boolean) {
        profileBioEditText.isEnabled = enabled
        profileUsernameEditText.isEnabled = enabled
        profileGenderEditText.isEnabled = enabled
        profileAgeEditText.isEnabled = enabled
        profileDobEditText.isEnabled = enabled
        profileMobileEditText.isEnabled = enabled
    }

    /**
     * Opens the gallery for image selection.
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMGUR_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMGUR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let { uploadImageToImgur(it) }
        }
    }

    /**
     * Uploads the selected image to Imgur.
     */
    private fun uploadImageToImgur(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val imageBytes = inputStream?.readBytes() ?: throw Exception("Failed to read image data")
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            val body = mapOf("image" to base64Image)

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.imgur.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val imgurApi = retrofit.create(ImgurApi::class.java)
            val call = imgurApi.uploadImage("Client-ID f41b5381b69da09", body)
            call.enqueue(object : Callback<ImgurResponse> {
                override fun onResponse(call: Call<ImgurResponse>, response: Response<ImgurResponse>) {
                    if (response.isSuccessful) {
                        uploadedImageUrl = response.body()?.data?.link
                        toast("Image uploaded: $uploadedImageUrl")
                        updateUserPhoto(uploadedImageUrl!!)
                        updateUserPhotoInRooms(uploadedImageUrl!!)
                    } else {
                        toast("Image upload failed: ${response.message()}")
                    }
                }
                override fun onFailure(call: Call<ImgurResponse>, t: Throwable) {
                    toast("Error: ${t.message}")
                }
            })
        } catch (e: Exception) {
            toast("Error uploading image: ${e.message}")
        }
    }

    /**
     * Updates the FirebaseAuth user profile and Firestore document with the new image URL.
     */
    private fun updateUserPhoto(photoUrl: String) {
        val user = auth.currentUser ?: return
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(Uri.parse(photoUrl))
            .build()

        user.updateProfile(profileUpdates)
            .addOnSuccessListener {
                db.collection("users").document(user.uid)
                    .update("photoUrl", photoUrl)
                    .addOnSuccessListener {
                        toast("Profile image updated!")
                        loadUserPhoto(photoUrl)
                    }
                    .addOnFailureListener { exception ->
                        toast("Failed to update Firestore: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                toast("Failed to update profile: ${exception.message}")
            }
    }
    private fun updateUserPhotoInRooms(photoUrl: String) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val roomsCollection = db.collection("rooms")
        roomsCollection.whereGreaterThan("participants.$userId.joinedOn", 0).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val roomId = document.id

                    val participantField = "participants.$userId.photoUrl"
                    roomsCollection.document(roomId)
                        .update(participantField, photoUrl)
                        .addOnSuccessListener {
                            toast("Updated photo in room: $roomId")
                        }
                        .addOnFailureListener { exception ->
                            toast("Failed to update photo in room $roomId: ${exception.message}")
                        }
                }
            }
            .addOnFailureListener { exception ->
                toast("Failed to get rooms: ${exception.message}")
            }
    }

    /**
     * Loads the profile image using Glide.
     */
    private fun loadUserPhoto(photoUrl: String) {
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_profile) // Ensure you have a fallback drawable (e.g. ic_profile)
            .error(R.drawable.ic_profile)
            .into(profileImageView)
    }

    /**
     * Saves updated profile data (bio, username, gender, age, DOB, mobile) to Firestore.
     */
    private fun saveProfileData() {
        val newBio = profileBioEditText.text.toString()
        val newUsername = profileUsernameEditText.text.toString()
        val newGender = profileGenderEditText.text.toString()
        val newAge = profileAgeEditText.text.toString().toLongOrNull() ?: 0
        val newDob = profileDobEditText.text.toString()
        val newMobile = profileMobileEditText.text.toString()

        val user: FirebaseUser? = auth.currentUser
        if (user == null) {
            toast("Please log in to update your profile.")
            navigateToLogin()
            return
        }

        db.collection("users").document(user.uid)
            .update(
                "bio", newBio,
                "username", newUsername,
                "gender", newGender,
                "age", newAge,
                "dob", newDob,
                "mobile", newMobile
            )
            .addOnSuccessListener {
                toast("Profile data updated successfully!")
                isDataEditMode = false
                setDataFieldsEnabled(false)
                editDataButton.text = "Edit Profile Data"
            }
            .addOnFailureListener { exception ->
                toast("Failed to update profile data: ${exception.message}")
            }
    }

    /**
     * Loads the current user's profile from FirebaseAuth and Firestore.
     */
    private fun loadCurrentUserProfile() {
        val user: FirebaseUser? = auth.currentUser
        if (user == null) {
            toast("Please log in to see your profile.")
            navigateToLogin()
            return
        }
        profileNameTextView.text = user.displayName ?: "N/A"
        profileEmailTextView.text = user.email ?: "N/A"
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    profileBioEditText.setText(document.getString("bio") ?: "")
                    profileUsernameEditText.setText(document.getString("username") ?: "N/A")
                    profileGenderEditText.setText(document.getString("gender") ?: "N/A")
                    profileAgeEditText.setText(document.getLong("age")?.toString() ?: "N/A")
                    profileDobEditText.setText(document.getString("dob") ?: "N/A")
                    profileMobileEditText.setText(document.getString("mobile") ?: "N/A")
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
     * Loads another user's profile from Firestore.
     */
    private fun loadUserProfile(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    profileNameTextView.text = document.getString("displayName") ?: "N/A"
                    profileEmailTextView.text = document.getString("email") ?: "N/A"
                    profileBioEditText.setText(document.getString("bio") ?: "")
                    profileUsernameEditText.setText(document.getString("username") ?: "N/A")
                    profileGenderEditText.setText(document.getString("gender") ?: "N/A")
                    profileAgeEditText.setText(document.getLong("age")?.toString() ?: "N/A")
                    profileDobEditText.setText(document.getString("dob") ?: "N/A")
                    profileMobileEditText.setText(document.getString("mobile") ?: "N/A")
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
     * Signs out the user from Firebase and Google, then navigates to the login screen.
     */
    private fun signOut() {
        auth.signOut()
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, googleSignInOptions)
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                toast("Signed out successfully!")
                navigateToLogin()
            }
        }
    }
}
