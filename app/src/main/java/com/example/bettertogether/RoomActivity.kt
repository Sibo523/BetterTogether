package com.example.bettertogether

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class RoomActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        viewPager.adapter = AdapterRoomPager(this)
    }
}
