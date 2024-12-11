package com.example.bettertogether

import android.app.Application
import com.google.firebase.FirebaseApp

// This class initializes Firebase when our app starts
class BetterTogetherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase before we try to use it
        FirebaseApp.initializeApp(this)
    }
}