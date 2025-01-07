package com.example.bettertogether

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlin.concurrent.thread

class HomeActivity : BaseActivity() {

    private lateinit var imgViaURL: ImageView
    private lateinit var progressDialog: ProgressDialog
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val profileButton = findViewById<ImageButton>(R.id.profile_button)
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // ImageView that presents the image
        imgViaURL = findViewById(R.id.imgViaURL)

        val newRoom = findViewById<Button>(R.id.new_room)
        newRoom.setOnClickListener { }

        val yourRooms = findViewById<Button>(R.id.yourRooms)
        yourRooms.setOnClickListener { }

        // Load image from URL (can replace the constant string with string variable)
        loadImageFromURL("https://i.imgur.com/1AOQjMn.jpeg")

        setupBottomNavigation()
    }


    private fun loadImageFromURL(imageUrl: String) {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Getting your pic....")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Perform network operation on a separate thread
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
                progressDialog.dismiss()
                if (bitmap != null) {
                    imgViaURL.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}
