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
package com.example.android.sunshine.utilities

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

/**
 * These utilities will be used to communicate with the weather servers.
 */
object NetworkUtils {

    private val TAG = NetworkUtils::class.simpleName

    private val DYNAMIC_WEATHER_URL = "https://andfun-weather.udacity.com/weather"

    private val STATIC_WEATHER_URL = "https://andfun-weather.udacity.com/staticweather"

    private val FORECAST_BASE_URL = STATIC_WEATHER_URL

    /*
     * NOTE: These values only effect responses from OpenWeatherMap, NOT from the fake weather
     * server. They are simply here to allow us to teach you how to build a URL if you were to use
     * a real API.If you want to connect your app to OpenWeatherMap's API, feel free to! However,
     * we are not going to show you how to do so in this course.
     */

    /* The format we want our API to return */
    private val format = "json"
    /* The units we want our API to return */
    private val units = "metric"
    /* The number of days we want our API to return */
    private val numDays = 14

    val QUERY_PARAM = "q"
    val LAT_PARAM = "lat"
    val LON_PARAM = "lon"
    val FORMAT_PARAM = "mode"
    val UNITS_PARAM = "units"
    val DAYS_PARAM = "cnt"

    /**
     * Builds the URL used to talk to the weather server using a location. This location is based
     * on the query capabilities of the weather provider that we are using.
     *
     * @param locationQuery The location that will be queried for.
     * @return The URL to use to query the weather server.
     */
    fun buildUrl(locationQuery: String): URL? {
        /** This will be implemented in a future lesson  */
        return null
    }

    /**
     * Builds the URL used to talk to the weather server using latitude and longitude of a
     * location.
     *
     * @param lat The latitude of the location
     * @param lon The longitude of the location
     * @return The Url to use to query the weather server.
     */
    fun buildUrl(lat: Double, lon: Double): URL? {
        /** This will be implemented in a future lesson  */
        return null
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */

    fun getResponseFromHttpUrl(url: URL): String? {
        val urlConnection = url.openConnection() as HttpURLConnection
        try {
            val inputStream = urlConnection.inputStream

            val scanner = Scanner(inputStream)
            scanner.useDelimiter("\\A")

            val hasInput = scanner.hasNext()
            return if (hasInput) {
                scanner.next()
            } else {
                null
            }
        } finally {
            urlConnection.disconnect()
        }
    }
}