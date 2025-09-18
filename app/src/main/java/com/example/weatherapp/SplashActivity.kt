package com.example.weatherapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user has already completed first launch
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val firstRunDone = prefs.getBoolean("first_run_done", false)

        if (firstRunDone) {
            // ✅ User has already logged in before, go to main app
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // ❌ First time user, show login screen
            startActivity(Intent(this, FirstLaunchActivity::class.java))
        }

        // Close splash so user can't return to it
        finish()
    }
}
