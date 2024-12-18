package com.example.bettertogether

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.FirebaseApp
import com.google.android.gms.security.ProviderInstaller
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import android.util.Log

class HomeActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance() // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        val profileButton = findViewById<ImageButton>(R.id.profile_button)
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        val newRoom = findViewById<Button>(R.id.new_room)
        newRoom.setOnClickListener { showFormDialog() }

        setupBottomNavigation()
    }

    // Dialog for the creation of a room
    private fun showFormDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.form_dialog, null)

        val checkbox = view.findViewById<CheckBox>(R.id.form_checkbox)
        val editText = view.findViewById<EditText>(R.id.form_text_input)
        val numberInput = view.findViewById<EditText>(R.id.form_number_input)
        val dateInput = view.findViewById<EditText>(R.id.form_date_input)
        val descriptionInput = view.findViewById<EditText>(R.id.form_description_input)
        val codeInput = view.findViewById<EditText>(R.id.form_code_input)
        val radioGroup = view.findViewById<RadioGroup>(R.id.form_radio_group)

        // Handle date picker
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

        val dialog = AlertDialog.Builder(this)
            .setTitle("Room Settings")
            .setView(view)
            .setPositiveButton("Submit", null) // We override this later to control dialog behavior
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            submitButton.setOnClickListener {
                // Perform validation
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
                    val termsOfService = checkbox.isChecked
                    val betSubject = editText.text.toString()
                    val betNumber = numberInput.text.toString()
                    val selectedDate = dateInput.text.toString()
                    val description = descriptionInput.text.toString()
                    val selectedRadio = view.findViewById<RadioButton>(selectedRadioId)?.text?.toString()

                    val user = auth.currentUser
                    if (user == null) {
                        Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Prepare data for Firestore
                    val roomData = hashMapOf(
                        "userId" to user.uid,
                        "termsAccepted" to termsOfService,
                        "betSubject" to betSubject,
                        "betNumber" to betNumber,
                        "description" to description,
                        "code" to code,
                        "selectedDate" to selectedDate,
                        "betType" to selectedRadio,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("rooms")
                        .add(roomData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Room added successfully!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss() // Close dialog on success
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        dialog.show()
    }

}
