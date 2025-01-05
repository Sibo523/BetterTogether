package com.example.bettertogether

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import com.google.firebase.firestore.FieldValue
import android.widget.*
import android.util.Log
import android.view.View
import java.util.*

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

    private lateinit var uploadImageButton: Button
    private var imageUri: String? = null // Store the uploaded image URI
    private var isEvent: Boolean = false

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

        uploadImageButton = Button(this).apply {
            text = "Upload Image"
            visibility = View.GONE
            setOnClickListener {
                openImagePicker() // Open image picker for upload
            }
        }
        formLayout.addView(uploadImageButton)

        checkbox_event.setOnCheckedChangeListener { _, isChecked ->
            isEvent = isChecked
            uploadImageButton.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        dateInput.setOnClickListener {
            showDatePicker()
        }

        submitButton.setOnClickListener {
            if (validateInputs()) {
                submitForm()
            }
        }

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
        if (codeInput.text.toString().length !in 6..10) {
            codeInput.error = "Code must be between 6 and 10 characters"
            isValid = false
        }
        if (radioGroup.checkedRadioButtonId == -1) {
            toast("Please select a ratio")
            isValid = false
        }
        if (isEvent && imageUri == null) {
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

        if (isEvent) {
            roomData["imageUri"] = imageUri
            db.collection("events")
                .add(roomData)
                .addOnSuccessListener { eventRef ->
                    addRoomToUser(userId,eventRef.id,betSubject,"owner",isPublic)
                }
                .addOnFailureListener { toast("Error adding event") }
        } else {
            db.collection("rooms")
                .add(roomData)
                .addOnSuccessListener { roomRef ->
                    addRoomToUser(userId,roomRef.id,betSubject,"owner",isPublic)
                }
                .addOnFailureListener { exception -> toast("Error adding room: ${exception.message}") }
            
        }
    }

    private fun openImagePicker() {
        // Open a file picker to select an image (example logic)
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, 100)
    }
    
}