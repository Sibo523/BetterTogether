package com.example.bettertogether

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

class ProfileActivity : BaseActivity() {

    // UI components for image
    private lateinit var profileImageView: ImageView
    private lateinit var editImageButton: ImageView
    private lateinit var profileNameTextView: TextView

    // UI components for profile data
    private lateinit var editDataButton: Button
    private lateinit var profileBioEditText: EditText
    private lateinit var profileUsernameEditText: EditText
    private lateinit var profileGenderEditText: EditText
    private lateinit var profileAgeEditText: EditText
    private lateinit var profileDobEditText: EditText
    private lateinit var profileMobileEditText: EditText
    private lateinit var profileEmailTextView: TextView

    // Imgur upload request codes
    private val IMGUR_GALLERY_REQUEST_CODE = 101
    private val CAMERA_REQUEST_CODE = 102
    var uploadedImageUrl: String? = null

    // Flag to track edit mode for profile data
    private var isDataEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        if (!isLoggedIn) {
            toast("Please log in to update your profile.")
            navigateToLogin()
            return
        }

        // Initialize image UI components
        profileImageView = findViewById(R.id.profile_image)
        editImageButton = findViewById(R.id.edit_image_button)
        profileNameTextView = findViewById(R.id.profile_name)

        // Initialize profile data UI components
        editDataButton = findViewById(R.id.edit_data_button)
        profileBioEditText = findViewById(R.id.profile_bio)
        profileUsernameEditText = findViewById(R.id.profile_username_value)
        profileGenderEditText = findViewById(R.id.profile_gender_value)
        profileAgeEditText = findViewById(R.id.profile_age_value)
        profileDobEditText = findViewById(R.id.profile_dob_value)
        profileMobileEditText = findViewById(R.id.profile_mobile_value)
        profileEmailTextView = findViewById(R.id.profile_email_value)

        // Set click listener on image edit icon to offer image source options
        editImageButton.setOnClickListener { showImagePickerOptions() }

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
        val otherUserId = intent.getStringExtra("USER_ID_KEY")
        if(!otherUserId.isNullOrBlank()){
            loadUserProfile(otherUserId)
            // Hide data edit button if viewing another user's profile
            editDataButton.visibility = android.view.View.GONE
        } else{ loadCurrentUserProfile() }

        // Set sign out button listener
        findViewById<Button>(R.id.sign_out_button).setOnClickListener { signOut() }
    }

    // Shows an AlertDialog to choose image source: Camera or Gallery.
    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()   // Option to capture a new photo
                    1 -> openGallery()  // Option to pick from gallery
                }
            }
            .show()
    }

    // Enables or disables editing for profile data text fields.
    private fun setDataFieldsEnabled(enabled: Boolean) {
        profileBioEditText.isEnabled = enabled
        profileUsernameEditText.isEnabled = enabled
        profileGenderEditText.isEnabled = enabled
        profileAgeEditText.isEnabled = enabled
        profileDobEditText.isEnabled = enabled
        profileMobileEditText.isEnabled = enabled
    }

    // Opens the gallery for image selection.
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMGUR_GALLERY_REQUEST_CODE)
    }

    // Opens the camera to capture a new photo.
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMGUR_GALLERY_REQUEST_CODE -> {
                    val selectedImageUri: Uri? = data?.data
                    selectedImageUri?.let { uploadImageToImgur(it) }
                }
                CAMERA_REQUEST_CODE -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    bitmap?.let { uploadImageToImgur(it) }
                }
            }
        }
    }

    // Uploads the selected image (from gallery) to Imgur.
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
                    } else{ toast("Image upload failed: ${response.message()}") }
                }
                override fun onFailure(call:Call<ImgurResponse>,t:Throwable){ toast("Error: ${t.message}") }
            })
        } catch(e:Exception){ toast("Error uploading image: ${e.message}") }
    }

    // Uploads the captured image (from camera) to Imgur.
    private fun uploadImageToImgur(bitmap: Bitmap) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageBytes = stream.toByteArray()
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

    // Updates Firestore document with the new image URL.
    private fun updateUserPhoto(photoUrl: String) {
        db.collection("users").document(userId)
            .update("photoUrl", photoUrl)
            .addOnSuccessListener {
                toast("Profile image updated!")
                loadUserPhoto(photoUrl)
            }
            .addOnFailureListener { exception ->
                toast("Failed to update Firestore: ${exception.message}")
            }
    }

    private fun updateUserPhotoInRooms(photoUrl: String) {
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

    // Loads the profile image using Glide.
    private fun loadUserPhoto(photoUrl: String) {
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_profile) // Ensure you have a fallback drawable (e.g. ic_profile)
            .error(R.drawable.ic_profile)
            .into(profileImageView)
    }

    // Saves updated profile data (bio, username, gender, age, DOB, mobile) to Firestore.
    private fun saveProfileData() {
        val newBio = profileBioEditText.text.toString()
        val newUsername = profileUsernameEditText.text.toString()
        val newGender = profileGenderEditText.text.toString()
        val newAge = profileAgeEditText.text.toString().toLongOrNull() ?: 0
        val newDob = profileDobEditText.text.toString()
        val newMobile = profileMobileEditText.text.toString()

        db.collection("users").document(userId)
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

    // Loads the current user's profile from Firestore.
    private fun loadCurrentUserProfile() {
        if (!isLoggedIn) {
            toast("Please log in to see your profile.")
            navigateToLogin()
            return
        }
        getUserName(userId) { userName ->
            profileNameTextView.text = userName
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        profileBioEditText.setHint(document.getString("bio") ?: "")
                        profileUsernameEditText.setHint(document.getString("username") ?: "N/A")
                        profileGenderEditText.setHint(document.getString("gender") ?: "N/A")
                        profileAgeEditText.setHint(document.getLong("age")?.toString() ?: "N/A")
                        profileDobEditText.setHint(document.getString("dob") ?: "N/A")
                        profileMobileEditText.setHint(document.getString("mobile") ?: "N/A")
                        profileEmailTextView.text = document.getString("email") ?: "N/A"
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
    }

    // Loads another user's profile from Firestore.
    private fun loadUserProfile(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    profileNameTextView.text = document.getString("displayName") ?: "N/A"
                    profileBioEditText.setText(document.getString("bio") ?: "")
                    profileUsernameEditText.setText(document.getString("username") ?: "N/A")
                    profileGenderEditText.setText(document.getString("gender") ?: "N/A")
                    profileAgeEditText.setText(document.getLong("age")?.toString() ?: "N/A")
                    profileDobEditText.setText(document.getString("dob") ?: "N/A")
                    profileMobileEditText.setText(document.getString("mobile") ?: "N/A")
                    profileEmailTextView.text = document.getString("email") ?: "N/A"
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

    // Signs out the user from Firebase and Google, then navigates to the login screen.
    private fun signOut() {
        auth.signOut()
        val googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        )
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {

                // Clear local data
                val editor = sharedPreferences.edit()
                editor.remove("userId")
                editor.remove("userEmail")
                editor.remove("userName")
                editor.apply()

                toast("Logged out successfully!")

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
