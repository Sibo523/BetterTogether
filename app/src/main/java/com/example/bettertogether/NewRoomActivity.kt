package com.example.bettertogether

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class NewRoomActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    // Declare views as class-level properties
    private lateinit var checkbox: CheckBox
    private lateinit var editText: EditText
    private lateinit var numberInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var formLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_room)

        auth = FirebaseAuth.getInstance()

        // Initialize views once
        checkbox = findViewById(R.id.form_checkbox)
        editText = findViewById(R.id.form_text_input)
        numberInput = findViewById(R.id.form_number_input)
        dateInput = findViewById(R.id.form_date_input)
        descriptionInput = findViewById(R.id.form_description_input)
        codeInput = findViewById(R.id.form_code_input)
        radioGroup = findViewById(R.id.form_radio_group)
        submitButton = findViewById(R.id.submit_button)
        formLayout = findViewById(R.id.form_layout)

        val additionalInputs = mutableListOf<EditText>()

        // Role check and dynamic UI
        auth.currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (isOwnerRole()) {
                    val eventCheckbox = CheckBox(this)
                    eventCheckbox.text = "Event?"
                    eventCheckbox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            // Show additional Event inputs
                            val extraField1 = createEditText("Event Title")
                            val extraField2 = createEditText("Event Description")
                            val extraField3 = createEditText("Event Location")
                            formLayout.addView(extraField1)
                            formLayout.addView(extraField2)
                            formLayout.addView(extraField3)
                            additionalInputs.add(extraField1)
                            additionalInputs.add(extraField2)
                            additionalInputs.add(extraField3)
                        } else {
                            // Remove additional inputs
                            additionalInputs.forEach { formLayout.removeView(it) }
                            additionalInputs.clear()
                        }
                    }
                    formLayout.addView(eventCheckbox, 6)
                }
            }
        }

        dateInput.setOnClickListener {
            showDatePicker()
        }

        submitButton.setOnClickListener {
            if (validateInputs()) {
                submitForm(additionalInputs)
            }
        }

        setupBottomNavigation()
    }

    private fun isOwnerRole(): Boolean {
        return true // Replace with actual logic
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
            Toast.makeText(this, "Please select a ratio", Toast.LENGTH_SHORT).show()
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

    private fun submitForm(additionalInputs: List<EditText>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val isPublic = checkbox.isChecked
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
                    "role" to "owner"
                )
            ) // Add creator as the first participant
        )

        if (additionalInputs.isNotEmpty()) {
            roomData["eventTitle"] = additionalInputs[0].text.toString()
            roomData["eventDescription"] = additionalInputs[1].text.toString()
            roomData["eventLocation"] = additionalInputs[2].text.toString()
        }

        // Add room to Firestore
        db.collection("rooms")
            .add(roomData)
            .addOnSuccessListener { roomRef ->
                val roomId = roomRef.id
                Toast.makeText(this, "Room added successfully!", Toast.LENGTH_SHORT).show()

                // Add room to the user's `rooms` subcollection
                val userRoomData = hashMapOf(
                    "roomId" to roomId,
                    "name" to betSubject,
                    "joinedOn" to System.currentTimeMillis(),
                    "role" to "owner" // Store the user's role in their subcollection
                )
                db.collection("users").document(userId).collection("rooms")
                    .document(roomId)
                    .set(userRoomData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Room linked to user successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error linking room to user: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error adding room: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createEditText(hint: String): EditText {
        val editText = EditText(this)
        editText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        editText.hint = hint
        return editText
    }
}