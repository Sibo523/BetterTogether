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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_room)

        auth = FirebaseAuth.getInstance()

        val checkbox = findViewById<CheckBox>(R.id.form_checkbox)
        val editText = findViewById<EditText>(R.id.form_text_input)
        val numberInput = findViewById<EditText>(R.id.form_number_input)
        val dateInput = findViewById<EditText>(R.id.form_date_input)
        val descriptionInput = findViewById<EditText>(R.id.form_description_input)
        val codeInput = findViewById<EditText>(R.id.form_code_input)
        val radioGroup = findViewById<RadioGroup>(R.id.form_radio_group)
        val submitButton = findViewById<Button>(R.id.submit_button)
        val formLayout = findViewById<LinearLayout>(R.id.form_layout)

        val additionalInputs = mutableListOf<EditText>()

        // Add Event Checkbox Dynamically for Users with Max Permissions
        val currentUser = auth.currentUser
        currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isAdmin = task.result?.claims?.get("admin") as? Boolean ?: false
                if (isAdmin) {
                    // Add "Event?" checkbox dynamically
                    val eventCheckbox = CheckBox(this)
                    eventCheckbox.text = "Event?"
                    eventCheckbox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            // Show additional inputs dynamically
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
                    formLayout.addView(eventCheckbox, 6) // Add after the last default input
                }
            }
        }

        dateInput.setOnClickListener {
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

        submitButton.setOnClickListener {
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
            val code = codeInput.text.toString()
            if (code.length !in 6..10) {
                codeInput.error = "Code must be between 6 and 10 characters"
                isValid = false
            }
            val selectedRadioId = radioGroup.checkedRadioButtonId
            if (selectedRadioId == -1) {
                Toast.makeText(this, "Please select a ratio", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (isValid) {
                val isPublic = checkbox.isChecked
                val betSubject = editText.text.toString()
                val betNumber = numberInput.text.toString()
                val selectedDate = dateInput.text.toString()
                val description = descriptionInput.text.toString()
                val selectedRadio = findViewById<RadioButton>(selectedRadioId)?.text?.toString()
                val roomData = hashMapOf(
                    "isPublic" to isPublic,
                    "name" to betSubject,
                    "betPoints" to betNumber,
                    "description" to description,
                    "code" to code,
                    "expiration" to selectedDate,
                    "betType" to selectedRadio,
                    "createdOn" to System.currentTimeMillis()
                )

                // Check if Event Fields Exist
                if (additionalInputs.isNotEmpty()) {
                    val eventData = roomData.toMutableMap()
                    eventData["eventTitle"] = additionalInputs[0].text.toString()
                    eventData["eventDescription"] = additionalInputs[1].text.toString()
                    eventData["eventLocation"] = additionalInputs[2].text.toString()

                    db.collection("events")
                        .add(eventData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Event added successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error adding event: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    db.collection("rooms")
                        .add(roomData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Room added successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error adding room: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
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
