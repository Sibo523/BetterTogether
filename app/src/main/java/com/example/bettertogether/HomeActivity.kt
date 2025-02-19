package com.example.bettertogether

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.DocumentSnapshot

import java.text.SimpleDateFormat
import java.util.*

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

    private val dailyRewards = listOf(100, 250, 400, 550, 750, 1000, 1500)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Slideshow ImageView
        imgViaURL = findViewById(R.id.imgViaURL)
        betButton = findViewById(R.id.bet)

        betButton.setOnClickListener {
            if(currentRoomId.isNotEmpty()){ openRoom(currentRoomId) }
            else{ toast("No event selected.") }
        }
        eventsForSlideshow()

        // Rooms Slider
        roomsSlider = findViewById(R.id.rooms_slider)
        roomsSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        roomsSliderAdapter = AdapterEvents(roomsList){ room -> openRoom(room.id) }
        roomsSlider.adapter = roomsSliderAdapter
        showPopularPublicRooms(roomsList,roomsSliderAdapter)

        setupCategoryClickListeners()
        checkDailyReward()
    }

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
                if(imageUrls.isNotEmpty()){ startSlideshow() }
                else{ toast("No images found in events.") }
            }
            .addOnFailureListener { exception -> toast("Failed to fetch events: ${exception.message}") }
    }
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

    private fun showDailyRewardDialog(pointsEarned: Int, streak: Int) {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.daily_reward_dialog, null)
        val rewardGrid = dialogView.findViewById<GridLayout>(R.id.rewardGrid)
        val rewardMessage = dialogView.findViewById<TextView>(R.id.rewardMessage)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        val dailyRewards = listOf(100, 250, 400, 550, 750, 1000, 1500)

        for (i in dailyRewards.indices) {
            val rewardText = TextView(this)
            rewardText.text = "${dailyRewards[i]}"
            rewardText.textSize = 14f
            rewardText.setPadding(12, 12, 12, 12)
            rewardText.gravity = android.view.Gravity.CENTER
            rewardText.setTypeface(null, Typeface.BOLD)
            rewardText.setBackgroundResource(R.drawable.reward_box)


            rewardText.setTextColor(Color.GRAY)
            if (i == (streak % dailyRewards.size)) {          // אם זה היום הנוכחי, הדגש אותו
                rewardText.setTextColor(Color.WHITE)
                rewardText.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple_500)
            } else if(i < (streak % dailyRewards.size)){      // הפחת שקיפות לימים הקודמים
                rewardText.alpha = 0.5f
            }

            val params = GridLayout.LayoutParams()
            params.setMargins(8, 8, 8, 8)
            params.width = LinearLayout.LayoutParams.WRAP_CONTENT
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT
            rewardText.layoutParams = params

            rewardGrid.addView(rewardText)
        }

        rewardMessage.text = "You received $pointsEarned points!\n See you tomorow!"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        okButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    fun checkDailyReward() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentPoints = document.getLong("currentPoints") ?: 0
                val lastLoginDate = document.getString("lastLoginDate") ?: "2000-01-01"
                val loginStreak = document.getLong("loginStreak") ?: 0

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if(today == lastLoginDate){ return@addOnSuccessListener }
                val newStreak = if(isYesterday(lastLoginDate)) loginStreak+1 else 1
                val rewardIndex = ((newStreak-1) % dailyRewards.size).toInt()
                val rewardPoints = dailyRewards[rewardIndex]
                val updatedPoints = currentPoints + rewardPoints
                userRef.update(
                    mapOf(
                        "currentPoints" to updatedPoints,
                        "lastLoginDate" to today,
                        "loginStreak" to newStreak
                    )
                ).addOnSuccessListener{ showDailyRewardDialog(rewardPoints,rewardIndex) }
                 .addOnFailureListener { toast("Failed to update daily rewards.") }
            }
        }.addOnFailureListener { toast("Error retrieving user data.") }
    }
    private fun isYesterday(lastLogin: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastLoginDate = sdf.parse(lastLogin) ?: return false
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return sdf.format(calendar.time) == sdf.format(lastLoginDate)
    }
}


class AdapterPopularRooms(
    private val rooms: List<Map<String, Any>>,
    private val onRoomClick: (String) -> Unit
) : RecyclerView.Adapter<AdapterPopularRooms.RoomViewHolder>() {
    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomName: TextView = view.findViewById(R.id.room_name)
        val participantsCount: TextView = view.findViewById(R.id.participants_count)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_popular_room, parent, false)
        return RoomViewHolder(view)
    }
    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        val roomName = room["name"] as? String ?: "Unnamed Room"
        val participantsCount = room["participantsCount"] as? Int ?: 0
        val maxParticipants = room["maxParticipants"] as? Int ?: 10 // Replace with a dynamic value if available

        holder.roomName.text = roomName
        holder.participantsCount.text = "$participantsCount/$maxParticipants"

        holder.itemView.setOnClickListener {
            val roomId = room["id"] as String
            onRoomClick(roomId) // Trigger the callback with the room ID
        }
    }
    override fun getItemCount(): Int {
        return rooms.size
    }
}
