package com.example.bettertogether

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        val yourRooms = findViewById<Button>(R.id.yourRooms)
        yourRooms.setOnClickListener { showFormDialog() }

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
        dateInput.setOnClickListener {                    // Handle date picker
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
                    val selectedRadio = view.findViewById<RadioButton>(selectedRadioId)?.text?.toString()
                    val user = auth.currentUser
                    if (user == null) {
                        Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val roomData = hashMapOf(  // Prepare data for Firestore
                        "isPublic" to isPublic,
                        "name" to betSubject,
                        "betPoints" to betNumber,
                        "description" to description,
                        "code" to code,
                        "expiration" to selectedDate,
                        "betType" to selectedRadio,
                        "createdOn" to System.currentTimeMillis()
                    )
                    db.collection("rooms")   // upload to DB
                        .add(roomData)
                        .addOnSuccessListener { roomDoc ->
                            // Add the user as a participant with role "owner"
                            val participantData = hashMapOf(
                                "role" to "owner",
                                "joinedAt" to System.currentTimeMillis()
                            )
                            roomDoc.collection("participants").document(user.uid)
                                .set(participantData)
                                .addOnSuccessListener {
                                    // Add the room to the user's "rooms" subcollection
                                    val userRoomData = hashMapOf(
                                        "role" to "owner",
                                        "joinedAt" to System.currentTimeMillis()
                                    )
                                    db.collection("users").document(user.uid).collection("rooms").document(roomDoc.id)
                                        .set(userRoomData)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Room added successfully!", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss() // Close dialog on success
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
        }
        dialog.show()
    }

//    private fun showYourRooms() {
//        val user = auth.currentUser
//        if (user == null) {
//            Toast.makeText(this, "Please log in to see your rooms.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val dialogView = layoutInflater.inflate(R.layout.rooms_list_dialog, null)
//        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rooms_recycler_view)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        val roomsList = mutableListOf<String>()
//        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, roomsList)
//        recyclerView.adapter = adapter
//
//        db.collection("users").document(user.uid).collection("rooms")
//            .get()
//            .addOnSuccessListener { documents ->
//                roomsList.clear()
//                for (document in documents) {
//                    roomsList.add(document.id)
//                }
//                adapter.notifyDataSetChanged()
//
//                AlertDialog.Builder(this)
//                    .setTitle("Your Rooms")
//                    .setView(dialogView)
//                    .setPositiveButton("OK", null)
//                    .show()
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error fetching rooms: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
}
