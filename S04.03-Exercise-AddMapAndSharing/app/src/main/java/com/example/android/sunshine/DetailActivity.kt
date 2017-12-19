package com.example.android.sunshine

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.android.sunshine.R.layout.activity_detail
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {
    private val FORECAST_SHARE_HASHTAG = " #SunshineApp"

    private var mForecast: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_detail)

        if (intent != null) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                mForecast = intent.getStringExtra(Intent.EXTRA_TEXT)
                tv_display_weather.text = mForecast
            }
        }
    }

    // TODO (3) Create a menu with an item with id of action_share
    // TODO (4) Display the menu and implement the forecast sharing functionality
}