package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class HomeActivity : BaseActivity() {
    private lateinit var roomsSlider: RecyclerView
    private lateinit var roomsSliderAdapter: AdapterPopularRooms
    private val roomsList = mutableListOf<Map<String, Any>>()

    private lateinit var imgViaURL: ImageView
    private val mainHandler = Handler(Looper.getMainLooper())
    private val imageUrls = mutableListOf<String>()
    private val eventRoomsMap = mutableMapOf<String, String>()
    private var currentImageIndex = 0
    private val slideshowInterval = 8000L // 8 seconds
    private var currentRoomId: String = ""
    private lateinit var betButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Slideshow ImageView
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

        // Rooms Slider
        roomsSlider = findViewById(R.id.rooms_slider)
        roomsSlider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        roomsSliderAdapter = AdapterPopularRooms(roomsList) { roomId ->
            openRoom(roomId)
        }
        roomsSlider.adapter = roomsSliderAdapter
        popularPublicRooms()

        setupCategoryClickListeners()
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

    private fun popularPublicRooms() {
        db.collection("rooms")
            .whereEqualTo("isEvent", false)
            .whereEqualTo("isPublic", true)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                roomsList.clear()
                roomsList.addAll(querySnapshot.documents.map { document ->
                    val roomsParticipants = getActiveParticipants(document)
                    val participantsCount = roomsParticipants.size
                    val maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 10

                    mapOf(
                        "id" to document.id,
                        "name" to (document.getString("name") ?: "Unnamed Room"),
                        "participantsCount" to participantsCount,
                        "maxParticipants" to maxParticipants,
                        "participants" to roomsParticipants
                    )
                }.sortedByDescending { it["participantsCount"] as? Int ?: 0 }) // מיון נכון גם לערכים null

                if (::roomsSliderAdapter.isInitialized) {
                    roomsSliderAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                toast("Error fetching popular rooms: ${exception.message}")
            }
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
