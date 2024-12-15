package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import android.app.DatePickerDialog
import java.util.Calendar


class ProfileActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        auth = FirebaseAuth.getInstance()

        val signOutButton = findViewById<Button>(R.id.sign_out_button)
        signOutButton.setOnClickListener{ signOut() }
        val new_room = findViewById<Button>(R.id.new_room)
        new_room.setOnClickListener{ showFormDialog() }
    }

    private fun showFormDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.form_dialog, null)

        val checkbox = view.findViewById<CheckBox>(R.id.form_checkbox)
        val editText = view.findViewById<EditText>(R.id.form_text_input)
        val numberInput = view.findViewById<EditText>(R.id.form_number_input)
        val dateInput = view.findViewById<EditText>(R.id.form_date_input) // Date input field
        val radioGroup = view.findViewById<RadioGroup>(R.id.form_radio_group)

        // Handle date picker
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // Format the date and set it in the EditText
                val formattedDate = "${selectedDay}/${selectedMonth + 1}/$selectedYear"
                dateInput.setText(formattedDate)
            }, year, month, day)

            datePicker.show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Submit Form")
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                val termsOfService = checkbox.isChecked
                val betSubject = editText.text.toString()
                val betNumber = numberInput.text.toString()
                val selectedDate = dateInput.text.toString()
                val selectedRadioId = radioGroup.checkedRadioButtonId
                val selectedRadio = findViewById<RadioButton>(selectedRadioId)?.text?.toString()

                if (betSubject.isBlank() || betNumber.isBlank() || selectedDate.isBlank() || selectedRadio==null){  // Validate inputs
                    Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
                } else{  // Display collected data
                    val message = """
                    Checkbox: $termsOfService
                    Bet Subject: $betSubject
                    Bet Number: $betNumber
                    Selected Date: $selectedDate
                    Selected Radio: $selectedRadio
                """.trimIndent()
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun signOut() {
        auth.signOut()
        // Redirect to LoginActivity
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show()
    }
}
