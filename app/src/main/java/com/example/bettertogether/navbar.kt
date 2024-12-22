package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBottomNavigation()
    }

    protected fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if(bottomNav == null){ return }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.nav_explorer -> {
                    navigateTo(ExplorerActivity::class.java)
                    true
                }
                R.id.nav_add -> {
                    navigateTo(NewRoomActivity::class.java)
                    true
                }
                R.id.nav_star -> {
                    navigateTo(RoomsActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    navigateTo(ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateTo(targetActivity: Class<out AppCompatActivity>) {
        if (this::class.java != targetActivity) {
            val intent = Intent(this, targetActivity)
            startActivity(intent)
        }
    }
    protected fun openRoom(roomId: String) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra("roomId", roomId)
        startActivity(intent)
    }
}
