package com.example.android.sunshine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

// COMPLETED (1) Create a new Activity called DetailActivity using Android Studio's wizard
class DetailActivity : AppCompatActivity() {
    companion object {
        private const val FORECAST_SHARE_HASHTAG = " #SunshineApp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
    }
}