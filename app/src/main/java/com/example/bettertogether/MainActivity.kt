package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()

        val signOutButton = findViewById<Button>(R.id.sign_out_button)
        signOutButton.setOnClickListener {
            signOut()
        }

        val new_room = findViewById<Button>(R.id.new_room)
        new_room.setOnClickListener {
            showFormDialog()
        }
    }

    private fun showFormDialog() {
        // Inflate the custom form layout
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.form_dialog, null)

        val checkbox = view.findViewById<CheckBox>(R.id.form_checkbox)
        val editText = view.findViewById<EditText>(R.id.form_text_input)
        val radioGroup = view.findViewById<RadioGroup>(R.id.form_radio_group)

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Submit Form")
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                val isChecked = checkbox.isChecked
                val textInput = editText.text.toString()
                val selectedRadioId = radioGroup.checkedRadioButtonId
                val selectedRadio = findViewById<RadioButton>(selectedRadioId)?.text?.toString()

                // Validate inputs
                if (textInput.isBlank() || selectedRadio == null) {
                    Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
                } else {
                    // Display collected data
                    val message = """
                        Checkbox: $isChecked
                        Text Input: $textInput
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
