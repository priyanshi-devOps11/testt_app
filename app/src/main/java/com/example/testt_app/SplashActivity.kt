package com.example.test_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.testt_app.R

// NOTE: R.layout.activity_splash is assumed to be defined in your project resources.

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for ~2 seconds before navigating to MainActivity
        // Using mainLooper ensures the handler runs on the UI thread
        Handler(mainLooper).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Finish the splash activity so the user cannot navigate back to it
            finish()
        }, 2000) // 2000 milliseconds = 2 seconds
    }
}