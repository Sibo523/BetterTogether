package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.util.Log

class HomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val profileButton = findViewById<ImageButton>(R.id.profile_button)
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        val newRoom = findViewById<Button>(R.id.new_room)
        newRoom.setOnClickListener { }

        val yourRooms = findViewById<Button>(R.id.yourRooms)
        yourRooms.setOnClickListener {  }

        setupBottomNavigation()
    }

}
