package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

import com.google.firebase.database.FirebaseDatabase

class HomeActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance() // Firebase Database Instance
    private val roomsRef = database.getReference("rooms") // Reference to 'rooms' node

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        auth = FirebaseAuth.getInstance()

        val new_room = findViewById<Button>(R.id.new_room)
        new_room.setOnClickListener { showFormDialog() }

        setupBottomNavigation()
    }

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
            .setPositiveButton("Submit") { _, _ ->
                val termsOfService = checkbox.isChecked
                val betSubject = editText.text.toString()
                val betNumber = numberInput.text.toString()
                val selectedDate = dateInput.text.toString()
                val description = descriptionInput.text.toString()
                val code = codeInput.text.toString()
                val selectedRadioId = radioGroup.checkedRadioButtonId
                val selectedRadio = view.findViewById<RadioButton>(selectedRadioId)?.text?.toString()

                if (betSubject.isBlank() || betNumber.isBlank() || selectedDate.isBlank() ||
                    description.isBlank() || code.length !in 6..10 || selectedRadio == null) {
                    Toast.makeText(this, "Please fill all fields correctly!", Toast.LENGTH_SHORT).show()
                } else {
                    // Prepare data for Firebase
                    val roomData = hashMapOf(
                        "termsAccepted" to termsOfService,
                        "betSubject" to betSubject,
                        "betNumber" to betNumber,
                        "description" to description,
                        "code" to code,
                        "selectedDate" to selectedDate,
                        "betType" to selectedRadio
                    )

                    // Push data to Firebase
                    val newRoomRef = roomsRef.push() // Generates a unique ID for the room
                    newRoomRef.setValue(roomData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Room added successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to add room. Try again.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }
}
