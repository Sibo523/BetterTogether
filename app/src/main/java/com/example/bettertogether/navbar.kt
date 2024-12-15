package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the activity includes a navigation bar, set up the listener
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.nav_explorer -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.nav_add -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.nav_star -> {
                    navigateTo(HomeActivity::class.java)
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
        if (this::class.java != targetActivity) { // Avoid reloading the same activity
            val intent = Intent(this, targetActivity)
            startActivity(intent)
            overridePendingTransition(0, 0) // Optional: Disable animation for seamless transition
        } else {
            Toast.makeText(this, "Already on this page!", Toast.LENGTH_SHORT).show()
        }
    }
}
