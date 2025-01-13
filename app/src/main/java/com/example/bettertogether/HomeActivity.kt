package com.example.bettertogether

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlin.concurrent.thread

class HomeActivity : BaseActivity() {

    private lateinit var imgViaURL: ImageView
    private lateinit var progressDialog: ProgressDialog
    private val mainHandler = Handler(Looper.getMainLooper())
    private val imageUrls = mutableListOf<String>()
    private var currentImageIndex = 0
    private val slideshowInterval = 8000L // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        imgViaURL = findViewById(R.id.imgViaURL)
        // Fetch events and start slideshow
        fetchEventsAndStartSlideshow()

        setupBottomNavigation()
    }

    private fun fetchEventsAndStartSlideshow() {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Fetching events...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        db.collection("rooms")
            .get()
            .addOnSuccessListener { querySnapshot ->
                progressDialog.dismiss()
                for (document in querySnapshot.documents) {
                    if(document.getBoolean("isEvent")==true){
                        val url = document.getString("url")
                        if (url != null) {
                            imageUrls.add(url)
                        }
                    }
                }

                if (imageUrls.isNotEmpty()) {
                    startSlideshow()
                } else {
                    Toast.makeText(this, "No images found in events.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to fetch events: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startSlideshow() {
        // Start rotating through images
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
        // Load image from URL in a separate thread
        thread {
            var bitmap: Bitmap? = null
            try {
                val inputStream: InputStream = URL(imageUrl).openStream()
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // Update UI on the main thread
            mainHandler.post {
                if (bitmap != null) {
                    imgViaURL.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load image: $imageUrl",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
