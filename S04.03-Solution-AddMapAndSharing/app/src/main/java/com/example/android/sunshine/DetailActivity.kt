package com.example.android.sunshine

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import com.example.android.sunshine.R.id.action_share
import com.example.android.sunshine.R.layout.activity_detail
import com.example.android.sunshine.R.menu.detail
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {
    private val FORECAST_SHARE_HASHTAG = " #SunshineApp"
    private lateinit var mForecast: String

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

    // COMPLETED (4) Display the menu and implement the forecast sharing functionality
    /**
     * Uses the ShareCompat Intent builder to create our Forecast intent for sharing. We set the
     * type of content that we are sharing (just regular text), the text itself, and we return the
     * newly created Intent.
     *
     * @return The Intent to use to start our share.
     */
    private fun createShareForecastIntent(): Intent {
        return ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(mForecast + FORECAST_SHARE_HASHTAG)
                .intent
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(detail, menu)
        val menuItem = menu.findItem(action_share)
        menuItem.intent = createShareForecastIntent()
        return true
    }
}