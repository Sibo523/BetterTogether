package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeActivity : BaseActivity() {

    private lateinit var roomsSlider: RecyclerView
    private lateinit var roomsSliderAdapter: AdapterPopularRooms
    private val roomsList = mutableListOf<Map<String, Any>>()

    private lateinit var imgViaURL: ImageView
    private val mainHandler = Handler(Looper.getMainLooper())
    private val imageUrls = mutableListOf<String>()
    private var currentImageIndex = 0
    private val slideshowInterval = 8000L // 8 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Slideshow ImageView
        imgViaURL = findViewById(R.id.imgViaURL)
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
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val url = document.getString("url")
                    if (url != null) {
                        imageUrls.add(url)
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
    private fun startSlideshow() {
        mainHandler.post(object : Runnable {
            override fun run() {
                if (imageUrls.isNotEmpty()) {
                    val imageUrl = imageUrls[currentImageIndex]
                    loadImageFromURL(imageUrl,imgViaURL)
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
                val categoryTextView = categoryLayout.getChildAt(1) as TextView  // ה-TextView שבתוך ה-LinearLayout

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
            .get()
            .addOnSuccessListener { querySnapshot ->
                roomsList.clear()
                roomsList.addAll(querySnapshot.documents.map { document ->
                    val participants = document.get("participants") as? Map<String, Map<String, Any>> ?: emptyMap()
                    val participantsCount = participants.size
                    val maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 10

                    mapOf(
                        "id" to document.id,
                        "name" to (document.getString("name") ?: "Unnamed Room"),
                        "participantsCount" to participantsCount,
                        "maxParticipants" to maxParticipants,
                        "participants" to participants
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
