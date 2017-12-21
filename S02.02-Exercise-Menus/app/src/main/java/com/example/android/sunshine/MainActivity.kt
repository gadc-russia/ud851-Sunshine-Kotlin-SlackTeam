/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.android.sunshine.data.getPreferredWeatherLocation
import com.example.android.sunshine.utilities.buildUrl
import com.example.android.sunshine.utilities.getResponseFromHttpUrl
import com.example.android.sunshine.utilities.getSimpleWeatherStringsFromJson
import kotlinx.android.synthetic.main.activity_forecast.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        /* Once all of our views are setup, we can load the weather data. */
        loadWeatherData()
    }

    /**
     * This method will get the user's preferred location for weather, and then tell some
     * background method to get the weather data in the background.
     */
    private fun loadWeatherData() {
        val location = getPreferredWeatherLocation(this)
        FetchWeatherTask().execute(location)
    }

    inner class FetchWeatherTask : AsyncTask<String, Void, Array<String>>() {

        override fun doInBackground(vararg params: String): Array<String>? {

            /* If there's no zip code, there's nothing to look up. */
            if (params.isEmpty()) {
                return null
            }

            val location = params[0]
            val weatherRequestUrl = buildUrl(location)

            return try {
                val jsonWeatherResponse = getResponseFromHttpUrl(weatherRequestUrl)
                getSimpleWeatherStringsFromJson(this@MainActivity, jsonWeatherResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }

        override fun onPostExecute(weatherData: Array<String>?) {
            if (weatherData != null) {
                /*
                 * Iterate through the array and append the Strings to the TextView. The reason why we add
                 * the "\n\n\n" after the String is to give visual separation between each String in the
                 * TextView. Later, we'll learn about a better way to display lists of data.
                 */
                for (weatherString in weatherData) {
                    tv_weather_data.append(weatherString + "\n\n\n")
                }
            }
        }
    }

    // TODO (2) Create a menu resource in res/menu/ called forecast.xml
    // TODO (3) Add one item to the menu with an ID of action_refresh
    // TODO (4) Set the title of the menu item to "Refresh" using strings.xml

    // TODO (5) Override onCreateOptionsMenu to inflate the menu for this Activity
    // TODO (6) Return true to display the menu

    // TODO (7) Override onOptionsItemSelected to handle clicks on the refresh button
}