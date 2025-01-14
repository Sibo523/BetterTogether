package com.example.bettertogether

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.concurrent.thread

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

        setupBottomNavigation()
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
                    Toast.makeText(this, "No images found in events.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch events: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startSlideshow() {
        mainHandler.post(object : Runnable {
            override fun run() {
                if (imageUrls.isNotEmpty()) {
                    val imageUrl = imageUrls[currentImageIndex]
                    loadImageFromURL(imageUrl)
                    currentImageIndex = (currentImageIndex + 1) % imageUrls.size
                }
                mainHandler.postDelayed(this, slideshowInterval)
            }
        })
    }

    private fun loadImageFromURL(imageUrl: String) {
        thread {
            var bitmap: Bitmap? = null
            try {
                val inputStream = java.net.URL(imageUrl).openStream()
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            mainHandler.post {
                if (bitmap != null) {
                    imgViaURL.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "Failed to load image: $imageUrl", Toast.LENGTH_SHORT).show()
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
                    val participants = document.get("participants") as? List<*> ?: emptyList<Any>()
                    mapOf(
                        "id" to document.id,
                        "name" to (document.getString("name") ?: "Unnamed Room"),
                        "participantsCount" to participants.size,
                        "maxParticipants" to (document.getString("maxParticipants")?.toIntOrNull() ?: 10),
                        "participants" to participants
                    )
                }.sortedByDescending { it["participantsCount"] as Int })
                roomsSliderAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching popular rooms: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
