package com.example.bettertogether

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

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

        dateInput.setOnClickListener { // Handle date picker
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
                val user = auth.currentUser
                if (user == null) {
                    Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

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

                db.collection("rooms")
                    .add(roomData)
                    .addOnSuccessListener { roomDoc ->
                        val participantData = hashMapOf(
                            "role" to "owner",
                            "joinedAt" to System.currentTimeMillis()
                        )
                        roomDoc.collection("participants").document(user.uid)
                            .set(participantData)
                            .addOnSuccessListener {
                                val userRoomData = hashMapOf(
                                    "role" to "owner",
                                    "joinedAt" to System.currentTimeMillis()
                                )
                                db.collection("users").document(user.uid).collection("rooms").document(roomDoc.id)
                                    .set(userRoomData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Room added successfully!", Toast.LENGTH_SHORT).show()
                                        finish() // Close activity on success
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(this, "Error adding room to user: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Error adding participant: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        setupBottomNavigation()
    }
}
