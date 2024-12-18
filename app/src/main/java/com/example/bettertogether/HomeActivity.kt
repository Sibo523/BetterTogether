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

        val newRoom = findViewById<Button>(R.id.new_room)
        newRoom.setOnClickListener { showFormDialog() }

        // Firebase Initialization
        Log.d("Debug", "Firebase initialization starting...")
        FirebaseApp.initializeApp(this)
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            Log.d("Debug", "Firebase initialized successfully!")
        } else {
            Log.e("Debug", "Firebase initialization failed!")
        }

        // Authentication Check
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            Log.d("Debug", "User signed in: ${user.uid}")
        } else {
            Log.d("Debug", "No user signed in.")
        }

        // Auth State Listener
        auth.addAuthStateListener {
            Log.d("Debug", "Auth state changed: ${it.currentUser?.uid ?: "No user"}")
        }

        // Firestore Write
        Log.d("Firestore", "Preparing to write data to Firestore...")
        val roomData = hashMapOf(
            "testField" to "testValue",
            "timestamp" to System.currentTimeMillis()
        )
        FirebaseFirestore.getInstance().collection("testCollection")
            .add(roomData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Document written successfully with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error writing document: ${e.message}", e)
            }
        Log.d("Firestore", "Firestore write operation executed.")

        // Firestore Read
        Log.d("Firestore", "Fetching data from Firestore...")
        FirebaseFirestore.getInstance().collection("testCollection").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d("Firestore", "Document fetched: ${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching documents: ${e.message}", e)
            }
        Log.d("Firestore", "Firestore read operation executed.")


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
                    val user = auth.currentUser
                    if (user == null) {
                        Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Prepare data for Firestore
                    val roomData = hashMapOf(
                        "userId" to user.uid,                       // Firebase UID
                        "termsAccepted" to termsOfService,          // accepted Terms?
                        "betSubject" to betSubject,                 // What's the bet?
                        "betNumber" to betNumber,                   // How many points
                        "description" to description,
                        "code" to code,
                        "selectedDate" to selectedDate,
                        "betType" to selectedRadio,
                        "timestamp" to System.currentTimeMillis()   // When uploaded
                    )
                    println("DEBUG: uploading")
                    db.collection("rooms")   // Upload data to Firestore in "rooms" collection
                        .add(roomData)
                        .addOnSuccessListener {
                            Toast.makeText(this,"Room added successfully!",Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this,"Error: ${exception.message}",Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }
}
