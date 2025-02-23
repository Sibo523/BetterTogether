package com.example.bettertogether

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.view.View
import java.util.*
import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import android.content.Context
import androidx.appcompat.app.AlertDialog
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap

/**
 * NewRoomActivity allows a user to create a new room (or event) with various options.
 * The activity includes options to set room details, upload an image (via camera or gallery),
 * and add multiple poll options.
 */
class NewRoomActivity : BaseActivity() {

    // UI components declared at class level
    private lateinit var checkbox_public: CheckBox
    private lateinit var checkbox_event: CheckBox
    private lateinit var roomNameText: EditText
    private lateinit var betAmountInput: EditText
    private lateinit var maxParticipantsInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var eventSubjectInput: EditText
    private lateinit var bet_type_radio: RadioGroup
    private lateinit var even_option: RadioButton
    private lateinit var ratio_option: RadioButton
    private lateinit var submitButton: Button
    private lateinit var uploadButton: Button   // Single button that provides both gallery and camera options
    private lateinit var pollOptionInput: EditText
    private lateinit var addPollOptionButton: Button
    private lateinit var pollOptionsList: RecyclerView
    // List holding poll options and its adapter
    private val pollOptions = mutableListOf<String>()
    private lateinit var pollOptionsAdapter: AdapterPollOptions

    // This variable holds the uploaded image URL; initialized with a default placeholder value.
    var uploadedImageUrl: String? = "null.png"

    /**
     * onCreate() is called when the activity is first created.
     * It initializes all UI components, sets click listeners, and handles view visibility.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_room)

        // Initialize UI components by finding them in the layout.
        checkbox_public = findViewById(R.id.form_checkbox_public)
        checkbox_event = findViewById(R.id.form_checkbox_event)
        roomNameText = findViewById(R.id.room_name)
        betAmountInput = findViewById(R.id.bet_amount_number)
        maxParticipantsInput = findViewById(R.id.max_participants_input)
        dateInput = findViewById(R.id.form_date_input)
        descriptionInput = findViewById(R.id.form_description_input)
        codeInput = findViewById(R.id.form_code_input)
        eventSubjectInput = findViewById(R.id.event_subject)
        bet_type_radio = findViewById(R.id.bet_type_radio)
        even_option = findViewById(R.id.event_option)
        ratio_option = findViewById(R.id.ratio_option)
        submitButton = findViewById(R.id.submit_button)
        uploadButton = findViewById(R.id.uploadButton)

        pollOptionInput = findViewById(R.id.poll_option_input)
        addPollOptionButton = findViewById(R.id.add_poll_option_button)
        pollOptionsList = findViewById(R.id.poll_options_list)
        pollOptionsAdapter = AdapterPollOptions(pollOptions)
        pollOptionsList.layoutManager = LinearLayoutManager(this)
        pollOptionsList.adapter = pollOptionsAdapter

        // Add new poll option when button is clicked.
        addPollOptionButton.setOnClickListener {
            val optionText = pollOptionInput.text.toString().trim()
            if (optionText.isNotEmpty()) {
                // Add the new poll option to the top of the list.
                pollOptions.add(0, optionText)
                pollOptionsAdapter.notifyItemInserted(0)
                pollOptionsList.scrollToPosition(0)
                pollOptionInput.text.clear()
            } else {
                toast("Option cannot be empty")
            }
        }

        // Check user role to determine whether to display the event checkbox.
        checkUserRole { role ->
            checkbox_event.visibility = if (role == "owner") View.VISIBLE else View.GONE
        }

        // Toggle visibility for the code input based on whether the room is public.
        checkbox_public.setOnCheckedChangeListener { _, isChecked ->
            codeInput.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
        // Toggle visibility for event subject input when event checkbox is changed.
        checkbox_event.setOnCheckedChangeListener { _, isChecked ->
            eventSubjectInput.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        // Toggle bet amount visibility based on selected bet type.
        ratio_option.setOnClickListener { betAmountInput.visibility = View.GONE }
        even_option.setOnClickListener { betAmountInput.visibility = View.VISIBLE }

        // Set the upload button to display image picker options.
        uploadButton.setOnClickListener { showImagePickerOptions() }

        // Set the date input to show a date picker when clicked.
        dateInput.setOnClickListener { showDatePicker() }

        // Set the submit button to validate and submit the form.
        submitButton.setOnClickListener {
            val allTexts = listOf(roomNameText.text.toString(), descriptionInput.text.toString()) + pollOptions
            checkBadWords(this, allTexts) { containsBadWords ->
                if (containsBadWords) {
                    toast("Your input contains inappropriate language.")
                } else if (validateInputs()) {
                    submitForm()
                }
            }
        }
    }

    /**
     * Displays an AlertDialog that lets the user choose between taking a photo or choosing one from the gallery.
     */
    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()   // Launch camera to capture a photo
                    1 -> openGallery()  // Open gallery to pick an image
                }
            }
            .show()
    }

    /**
     * Checks for bad words by sending the combined text to an external service.
     *
     * @param context The context from which the request is made.
     * @param texts A list of strings to check for profanity.
     * @param callback A callback function returning true if profanity is detected.
     */
    private fun checkBadWords(context: Context, texts: List<String>, callback: (Boolean) -> Unit) {
        val combinedText = texts.joinToString(" ")
        val url = "https://www.purgomalum.com/service/containsprofanity?text=${combinedText}"

        val request = StringRequest(Request.Method.GET, url,
            { response -> callback(response.toBoolean()) },
            { _ -> callback(false) })

        Volley.newRequestQueue(context).add(request)
    }

    /**
     * Validates form inputs before submission.
     * Checks for empty fields, incorrect code lengths, and ensures minimum poll options.
     *
     * @return true if all inputs are valid; false otherwise.
     */
    private fun validateInputs(): Boolean {
        if (roomNameText.text.toString().isBlank()) {
            roomNameText.error = "Bet subject cannot be empty"
            return false
        }
        if (pollOptions.size < 2) {
            pollOptionInput.error = "Add at least 2 options"
            return false
        }
        if (even_option.isChecked && (betAmountInput.text.toString().isBlank() ||
                    (betAmountInput.text.toString().toIntOrNull() ?: 0) < 10)) {
            betAmountInput.error = "Bet number cannot be empty for Even bet"
            return false
        }
        if (dateInput.text.toString().isBlank()) {
            dateInput.error = "Please select a date"
            return false
        }
        if (descriptionInput.text.toString().isBlank()) {
            descriptionInput.error = "Description cannot be empty"
            return false
        }
        if (maxParticipantsInput.text.toString().isBlank()) {
            maxParticipantsInput.error = "Max Participants cannot be empty"
            return false
        }
        if (!checkbox_public.isChecked && codeInput.text.toString().length !in 6..10) {
            codeInput.error = "Code must be between 6 and 10 characters"
            return false
        }
        if (!even_option.isChecked && !ratio_option.isChecked) {
            toast("Please select a ratio")
            return false
        }
        if (checkbox_event.isChecked && eventSubjectInput.text.toString().isBlank()) {
            toast("Event should have a subject")
            return false
        }
        if (checkbox_event.isChecked && (uploadedImageUrl == "null.png" || uploadedImageUrl.isNullOrEmpty())) {
            toast("Please upload an image or wait for the event")
            return false
        }
        return true
    }

    /**
     * Displays a DatePickerDialog to allow the user to select a date.
     * The selected date is then set into the dateInput field.
     */
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

    /**
     * Submits the room creation form to Firestore.
     * It first checks if the user is logged in, collects all form data, and then stores it in Firestore.
     */
    private fun submitForm() {
        if (!isLoggedIn) {
            toast("Please log in to create a room.")
            navigateToLogin()
            return
        }

        // Collect form data from input fields.
        val isPublic = checkbox_public.isChecked
        val isEvent = checkbox_event.isChecked
        val betSubject = roomNameText.text.toString()
        val betAmount = betAmountInput.text.toString().toIntOrNull() ?: 0
        val maxParticipants = maxParticipantsInput.text.toString().toIntOrNull() ?: 10
        val selectedDate = dateInput.text.toString()
        val description = descriptionInput.text.toString()
        val selectedRadio = findViewById<RadioButton>(bet_type_radio.checkedRadioButtonId)?.text?.toString()
        val code = codeInput.text.toString()
        val eventSubject = eventSubjectInput.text.toString()

        // Get user name and photo URL then create room data.
        getUserName(userId) { userName ->
            getUserPhotoUrl(userId) { userUrl ->
                val url = uploadedImageUrl.toString()
                // Build a map of participants with the current user as the owner.
                val participantsMap = hashMapOf(
                    userId to hashMapOf(
                        "name" to userName,
                        "role" to "owner",
                        "photoUrl" to userUrl,
                        "isBetting" to false,
                        "joinedOn" to System.currentTimeMillis(),
                        "isActive" to true
                    )
                )
                // Build room data to be stored in Firestore.
                val roomData = hashMapOf(
                    "isEvent" to isEvent,
                    "isPublic" to isPublic,
                    "name" to betSubject,
                    "options" to pollOptions,
                    "betPoints" to betAmount,
                    "maxParticipants" to maxParticipants,
                    "description" to description,
                    "code" to code,
                    "eventSubject" to eventSubject,
                    "expiration" to selectedDate,
                    "betType" to selectedRadio,
                    "createdOn" to System.currentTimeMillis(),
                    "createdBy" to userId,
                    "url" to url,
                    "participants" to participantsMap,
                    "isActive" to true
                )
                // Add the room to the Firestore "rooms" collection.
                db.collection("rooms")
                    .add(roomData)
                    .addOnSuccessListener { roomRef ->
                        addRoomToUser(userId, roomRef.id, betSubject, isPublic) { success ->
                            if (success) {
                                toast("Created room successfully")
                                openRoom(roomRef.id)
                            }
                        }
                    }
                    .addOnFailureListener { toast("Error adding room") }
            }
        }
    }

    /**
     * Opens the gallery to allow the user to select an image.
     * Checks that the user is logged in before proceeding.
     */
    private fun openGallery() {
        if (!isLoggedIn) {
            toast("Please log in to upload an image.")
            navigateToLogin()
            return
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 101)
    }

    /**
     * Opens the camera to capture a new photo.
     * Checks that the user is logged in before proceeding.
     */
    private fun openCamera() {
        if (!isLoggedIn) {
            toast("Please log in to upload an image.")
            navigateToLogin()
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 102)
    }

    /**
     * Handles the results from activities started for result (gallery or camera).
     * Depending on the request code, the appropriate image upload method is called.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            // Image selected from gallery.
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {
                uploadImageToImgur(it)
            }
        } else if (requestCode == 102 && resultCode == Activity.RESULT_OK) {
            // Image captured from camera as a Bitmap.
            val bitmap = data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                uploadImageToImgur(it)
            }
        }
    }

    /**
     * Uploads an image (selected from the gallery) to Imgur.
     * The image is converted to a Base64 string and sent via Retrofit.
     *
     * @param uri The Uri of the image selected from the gallery.
     */
    private fun uploadImageToImgur(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val imageBytes = inputStream?.readBytes() ?: throw Exception("Failed to read image data")
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            val body = mapOf("image" to base64Image)

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.imgur.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val imgurApi = retrofit.create(ImgurApi::class.java)

            val call = imgurApi.uploadImage("Client-ID f41b5381b69da09", body)
            call.enqueue(object : Callback<ImgurResponse> {
                override fun onResponse(call: Call<ImgurResponse>, response: Response<ImgurResponse>) {
                    if (response.isSuccessful) {
                        uploadedImageUrl = response.body()?.data?.link
                        Toast.makeText(this@NewRoomActivity, "Uploaded: $uploadedImageUrl", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@NewRoomActivity, "Upload failed: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ImgurResponse>, t: Throwable) {
                    Toast.makeText(this@NewRoomActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Uploads an image (captured from the camera as a Bitmap) to Imgur.
     * The Bitmap is compressed, converted to a Base64 string, and sent via Retrofit.
     *
     * @param bitmap The Bitmap image captured from the camera.
     */
    private fun uploadImageToImgur(bitmap: Bitmap) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageBytes = stream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            val body = mapOf("image" to base64Image)

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.imgur.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val imgurApi = retrofit.create(ImgurApi::class.java)

            val call = imgurApi.uploadImage("Client-ID f41b5381b69da09", body)
            call.enqueue(object : Callback<ImgurResponse> {
                override fun onResponse(call: Call<ImgurResponse>, response: Response<ImgurResponse>) {
                    if (response.isSuccessful) {
                        uploadedImageUrl = response.body()?.data?.link
                        Toast.makeText(this@NewRoomActivity, "Uploaded: $uploadedImageUrl", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@NewRoomActivity, "Upload failed: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ImgurResponse>, t: Throwable) {
                    Toast.makeText(this@NewRoomActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * AdapterPollOptions is a RecyclerView.Adapter that displays a list of poll options.
 * Each item includes the option text and a remove button to delete the option.
 */
class AdapterPollOptions(
    private val options: MutableList<String>
) : RecyclerView.Adapter<AdapterPollOptions.PollOptionViewHolder>() {

    /**
     * PollOptionViewHolder holds references to the UI components in each poll option item.
     */
    class PollOptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionText: TextView = view.findViewById(R.id.option_text)
        val removeButton: ImageButton = view.findViewById(R.id.remove_option_button)
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollOptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poll_option, parent, false)
        return PollOptionViewHolder(view)
    }

    /**
     * Binds data (poll option text) to the ViewHolder and sets the remove button listener.
     */
    override fun onBindViewHolder(holder: PollOptionViewHolder, position: Int) {
        holder.optionText.text = options[position]
        holder.removeButton.setOnClickListener {
            options.removeAt(position)
            notifyDataSetChanged()
        }
    }

    /**
     * Returns the total number of poll options.
     */
    override fun getItemCount(): Int = options.size
}
