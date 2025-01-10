package com.example.bettertogether

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.util.Log
import android.view.View
import java.util.*
import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewRoomActivity : BaseActivity() {

    // Declare views as class-level properties
    private lateinit var checkbox_public: CheckBox
    private lateinit var checkbox_event: CheckBox
    private lateinit var editText: EditText
    private lateinit var numberInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var formLayout: LinearLayout
    private lateinit var uploadButton: Button
    var uploadedImageUrl: String? = "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_room)

        // Initialize views once
        checkbox_public = findViewById(R.id.form_checkbox_public)
        checkbox_event = findViewById(R.id.form_checkbox_event)
        editText = findViewById(R.id.form_text_input)
        numberInput = findViewById(R.id.form_number_input)
        dateInput = findViewById(R.id.form_date_input)
        descriptionInput = findViewById(R.id.form_description_input)
        codeInput = findViewById(R.id.form_code_input)
        radioGroup = findViewById(R.id.form_radio_group)
        submitButton = findViewById(R.id.submit_button)
        formLayout = findViewById(R.id.form_layout)
        uploadButton = findViewById(R.id.uploadButton)

        checkUserRole { role ->
            checkbox_event.visibility = if(role=="owner") View.VISIBLE else View.GONE
        }
        checkbox_event.setOnCheckedChangeListener { _, isChecked ->
            uploadButton.visibility = if(isChecked) View.VISIBLE else View.GONE
        }
        checkbox_public.setOnCheckedChangeListener { _, isChecked ->
            codeInput.visibility = if(isChecked) View.GONE else View.VISIBLE
        }

        uploadButton.setOnClickListener{ openGallery() }

        dateInput.setOnClickListener{ showDatePicker() }

        submitButton.setOnClickListener{ if(validateInputs()){ submitForm() } }

        setupBottomNavigation()
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (editText.text.toString().isBlank()) {
            editText.error = "Bet subject cannot be empty"
            isValid = false
        }
        if (numberInput.text.toString().isBlank()) {
            numberInput.error = "Bet number cannot be empty"
            isValid = false
        }
        if (dateInput.text.toString().isBlank()) {
            dateInput.error = "Please select a date"
            isValid = false
        }
        if (descriptionInput.text.toString().isBlank()) {
            descriptionInput.error = "Description cannot be empty"
            isValid = false
        }
        if (!checkbox_public.isChecked && codeInput.text.toString().length !in 6..10) {
            codeInput.error = "Code must be between 6 and 10 characters"
            isValid = false
        }
        if (radioGroup.checkedRadioButtonId == -1) {
            toast("Please select a ratio")
            isValid = false
        }
        if (checkbox_event.isChecked && uploadedImageUrl == "null") {
            toast("Please upload an image for the event")
            isValid = false
        }

        return isValid
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "${selectedDay}/${selectedMonth + 1}/$selectedYear"
            dateInput.setText(formattedDate)
        }, year, month, day)
        datePicker.show()
    }

    private fun submitForm() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            toast("User not authenticated")
            return
        }

        val isPublic = checkbox_public.isChecked
        val betSubject = editText.text.toString()
        val betNumber = numberInput.text.toString()
        val selectedDate = dateInput.text.toString()
        val description = descriptionInput.text.toString()
        val selectedRadio = findViewById<RadioButton>(radioGroup.checkedRadioButtonId)?.text?.toString()
        val code = codeInput.text.toString()
        val userId = currentUser.uid
        val userName = currentUser.displayName ?: currentUser.email ?: "Anonymous"
        val url = uploadedImageUrl.toString()

        val roomData = hashMapOf(
            "isPublic" to isPublic,
            "name" to betSubject,
            "betPoints" to betNumber,
            "description" to description,
            "code" to code,
            "expiration" to selectedDate,
            "betType" to selectedRadio,
            "createdOn" to System.currentTimeMillis(),
            "createdBy" to userId,
            "participants" to mutableListOf(
                hashMapOf(
                    "id" to userId,
                    "name" to userName,
                    "role" to "owner",
                    "joinedOn" to System.currentTimeMillis()
                )
            )
        )

        var whereToAdd = "rooms"
        if (checkbox_event.isChecked) {
            roomData["url"] = url
            whereToAdd = "events"
        }
        db.collection(whereToAdd)
            .add(roomData)
            .addOnSuccessListener { roomRef ->
                addRoomToUser(userId,roomRef.id,betSubject,"owner",isPublic) { success ->
                    if(success){
                        toast("Created room successfully")
                        openRoom(roomRef.id)
                    }
                }
            }
            .addOnFailureListener { toast("Error adding room") }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {
                uploadImageToImgur(it)
            }
        }
    }

    private fun uploadImageToImgur(uri: Uri) {
        try {
            // Get InputStream from the Uri
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
                        Toast.makeText(this@NewRoomActivity, "Uploaded: $uploadedImageUrl", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@NewRoomActivity, "Upload failed: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ImgurResponse>, t: Throwable) {
                    Toast.makeText(this@NewRoomActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
