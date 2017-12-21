package com.example.android.sunshine

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {
    private val FORECAST_SHARE_HASHTAG = " #SunshineApp"
    private lateinit var mForecast: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // COMPLETED (2) Display the weather forecast that was passed from MainActivity
        if (intent != null) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                mForecast = intent.getStringExtra(Intent.EXTRA_TEXT)
                tv_display_weather.text = mForecast
            }
        }
    }
}