/**
 * HomeActivity displays the main screen of the app, featuring a slideshow of event images,
 * a horizontal slider for popular public rooms, category-based navigation, and daily reward management.
 *
 * Functionality Overview:
 *
 * - onCreate(savedInstanceState: Bundle?):
 *     - Sets the activity layout.
 *     - Initializes UI components including the slideshow ImageView, bet button, and rooms slider.
 *     - Configures the bet button to open a room if an event is selected.
 *     - Initiates fetching of event rooms for the slideshow.
 *     - Sets up the horizontal rooms slider to display popular public rooms.
 *     - Attaches click listeners to subject category rows for navigation to subject-specific events.
 *     - Checks for and processes daily reward eligibility.
 *
 * - eventsForSlideshow():
 *     - Queries Firestore for event rooms that are active.
 *     - Populates lists of image URLs and maps each URL to its corresponding room ID.
 *     - Starts the slideshow if images are available; otherwise, displays a notification.
 *
 * - startSlideshow():
 *     - Uses a Handler to cycle through event images at a fixed interval (8 seconds).
 *     - Loads each image into the slideshow ImageView and updates the current room ID.
 *
 * - setupCategoryClickListeners():
 *     - Iterates over predefined category rows (e.g., sports and other subjects).
 *     - Sets click listeners on each category to launch an activity that displays events for that subject.
 *
 * - showDailyRewardDialog(pointsEarned: Int, streak: Int):
 *     - Inflates and configures a dialog layout to display daily reward details.
 *     - Displays a grid of reward amounts, highlighting the current day's reward based on the user’s streak.
 *     - Shows a message with the points earned and waits for user acknowledgment.
 *
 * - checkDailyReward():
 *     - Retrieves the user's document from Firestore to check the current points, last login date, and login streak.
 *     - Determines if today's reward has already been claimed.
 *     - If eligible, calculates the new login streak and reward amount, updates the user data, and shows the reward dialog.
 *     - Schedules notifications for the next reward cycle.
 *
 * - isYesterday(lastLogin: String): Boolean
 *     - Helper method to verify if the provided last login date corresponds to yesterday.
 *
 * - scheduleDailyRewardNotifications():
 *     - Initiates scheduling of two daily alarms (at 9:00 AM and 9:00 PM) for daily reward notifications.
 *
 * - scheduleAlarmForTime(hourOfDay: Int, minute: Int, requestCode: Int):
 *     - Sets up a repeating alarm via AlarmManager to trigger at the specified time daily.
 *     - Uses a PendingIntent linked to a broadcast receiver (DailyRewardReceiver) to deliver the alarm.
 */


package com.example.bettertogether

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class HomeActivity : BaseActivity() {
    private lateinit var roomsSlider: RecyclerView
    private lateinit var roomsSliderAdapter: AdapterEvents
    private val roomsList = mutableListOf<DocumentSnapshot>()

    private lateinit var imgViaURL: ImageView
    private val mainHandler = Handler(Looper.getMainLooper())
    private val imageUrls = mutableListOf<String>()
    private val eventRoomsMap = mutableMapOf<String, String>()
    private var currentImageIndex = 0
    private val slideshowInterval = 8000L // 8 seconds
    private var currentRoomId: String = ""
    private lateinit var betButton: Button

    // Daily rewards for each day in the streak.
    private val dailyRewards = listOf(100, 250, 400, 550, 750, 1000, 1500)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Setup the slideshow ImageView and bet button.
        imgViaURL = findViewById(R.id.imgViaURL)
        betButton = findViewById(R.id.bet)
        betButton.setOnClickListener {
            if (currentRoomId.isNotEmpty()) {
                openRoom(currentRoomId)
            } else {
                toast("No event selected.")
            }
        }
        eventsForSlideshow()

        // Rooms Slider configuration.
        roomsSlider = findViewById(R.id.rooms_slider)
        roomsSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        roomsSliderAdapter = AdapterEvents(roomsList) { room -> openRoom(room.id) }
        roomsSlider.adapter = roomsSliderAdapter
        showPopularPublicRooms(roomsList, roomsSliderAdapter)

        setupCategoryClickListeners()

        // Check if a new daily reward is available and update if necessary.
        checkDailyReward()
    }

    // Fetches event rooms for the slideshow.
    private fun eventsForSlideshow() {
        db.collection("rooms")
            .whereEqualTo("isEvent", true)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                imageUrls.clear()
                eventRoomsMap.clear()
                for (document in querySnapshot.documents) {
                    val url = document.getString("url")
                    val roomId = document.id
                    if (url != null) {
                        imageUrls.add(url)
                        eventRoomsMap[url] = roomId
                    }
                }
                if (imageUrls.isNotEmpty()) {
                    startSlideshow()
                } else {
                    toast("No images found in events.")
                }
            }
            .addOnFailureListener { exception ->
                toast("Failed to fetch events: ${exception.message}")
            }
    }

    // Starts the slideshow that cycles through event images.
    private fun startSlideshow() {
        mainHandler.post(object : Runnable {
            override fun run() {
                if (imageUrls.isNotEmpty()) {
                    val imageUrl = imageUrls[currentImageIndex]
                    loadImageFromURL(imageUrl, imgViaURL)
                    currentRoomId = eventRoomsMap[imageUrl] ?: ""
                    currentImageIndex = (currentImageIndex + 1) % imageUrls.size
                }
                mainHandler.postDelayed(this, slideshowInterval)
            }
        })
    }

    // Sets up click listeners for the subject category rows.
    private fun setupCategoryClickListeners() {
        val categoryRows = listOf(
            findViewById<LinearLayout>(R.id.sports_row),
            findViewById<LinearLayout>(R.id.other_subjects_row)
        )

        for (row in categoryRows) {
            for (i in 0 until row.childCount) {
                val categoryLayout = row.getChildAt(i) as LinearLayout
                val categoryTextView = categoryLayout.getChildAt(1) as TextView

                categoryLayout.setOnClickListener {
                    val subject = categoryTextView.tag.toString()
                    val intent = Intent(this, EventsBySubjectActivity::class.java).apply {
                        putExtra("subject", subject)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    // Displays a dialog showing the daily reward details.
    private fun showDailyRewardDialog(pointsEarned: Int, streak: Int) {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.daily_reward_dialog, null)
        val rewardGrid = dialogView.findViewById<GridLayout>(R.id.rewardGrid)
        val rewardMessage = dialogView.findViewById<TextView>(R.id.rewardMessage)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        // Create a grid of TextViews to display the daily rewards.
        for (i in dailyRewards.indices) {
            val rewardText = TextView(this)
            rewardText.text = "${dailyRewards[i]}"
            rewardText.textSize = 14f
            rewardText.setPadding(12, 12, 12, 12)
            rewardText.gravity = android.view.Gravity.CENTER
            rewardText.setTypeface(null, Typeface.BOLD)
            rewardText.setBackgroundResource(R.drawable.reward_box)
            rewardText.setTextColor(Color.GRAY)
            if (i == (streak % dailyRewards.size)) { // Highlight today's reward.
                rewardText.setTextColor(Color.WHITE)
                rewardText.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple_500)
            } else if (i < (streak % dailyRewards.size)) { // Dim previous days.
                rewardText.alpha = 0.5f
            }
            val params = GridLayout.LayoutParams()
            params.setMargins(8, 8, 8, 8)
            params.width = LinearLayout.LayoutParams.WRAP_CONTENT
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT
            rewardText.layoutParams = params

            rewardGrid.addView(rewardText)
        }

        rewardMessage.text = "You received $pointsEarned points!\nSee you tomorrow!"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        okButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // Checks if the user is eligible for a new daily reward.
    fun checkDailyReward() {
        if (!isLoggedIn) { return }
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentPoints = document.getLong("currentPoints") ?: 0
                val lastLoginDate = document.getString("lastLoginDate") ?: "2000-01-01"
                val loginStreak = document.getLong("loginStreak") ?: 0

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (today == lastLoginDate) {
                    // Today's reward has already been claimed.
                    // Schedule notifications for tomorrow.
                    scheduleDailyRewardNotifications()
                    return@addOnSuccessListener
                }
                val newStreak = if (isYesterday(lastLoginDate)) loginStreak + 1 else 1
                val rewardIndex = ((newStreak - 1) % dailyRewards.size).toInt()
                val rewardPoints = dailyRewards[rewardIndex]
                val updatedPoints = currentPoints + rewardPoints
                userRef.update(
                    mapOf(
                        "currentPoints" to updatedPoints,
                        "lastLoginDate" to today,
                        "loginStreak" to newStreak
                    )
                ).addOnSuccessListener {
                    showDailyRewardDialog(rewardPoints, rewardIndex)
                    // After claiming today's reward, schedule notifications for the next cycle.
                    scheduleDailyRewardNotifications()
                }.addOnFailureListener { toast("Failed to update daily rewards.") }
            }
        }.addOnFailureListener { toast("Error retrieving user data.") }
    }

    // Checks if the given date is yesterday relative to today. Returns true if it is.
    private fun isYesterday(lastLogin: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastLoginDate = sdf.parse(lastLogin) ?: return false
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return sdf.format(calendar.time) == sdf.format(lastLoginDate)
    }

    // Schedules notifications for the next available reward times (tomorrow at 9:00 AM and 9:00 PM).
    private fun scheduleDailyRewardNotifications() {
        lifecycleScope.launch {
            scheduleAlarmForTime(9, 0, 0)  // 9:00 AM notification.
            scheduleAlarmForTime(21, 0, 1) // 9:00 PM notification.
        }
    }

    // Helper method that sets a repeating alarm at the specified time.
    private fun scheduleAlarmForTime(hourOfDay: Int, minute: Int, requestCode: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // If the specified time has already passed today, schedule for tomorrow.
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val intent = Intent(this, DailyRewardReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
