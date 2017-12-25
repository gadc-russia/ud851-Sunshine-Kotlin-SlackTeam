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
package com.example.android.sunshine.data

import android.provider.BaseColumns

/**
 * Defines table and column names for the weather database. This class is not necessary, but keeps
 * the code organized.
 */
class WeatherContract {

    /* Inner class that defines the table contents of the weather table */
    object WeatherEntry : BaseColumns {
        const val _ID = "_id"

        /* Used internally as the name of our weather table. */
        const val TABLE_NAME = "weather"

        /*
         * The date column will store the UTC date that correlates to the local date for which
         * each particular weather row represents. For example, if you live in the Eastern
         * Standard Time (EST) time zone and you load weather data at 9:00 PM on September 23, 2016,
         * the UTC time stamp for that particular time would be 1474678800000 in milliseconds.
         * However, due to time zone offsets, it would already be September 24th, 2016 in the GMT
         * time zone when it is 9:00 PM on the 23rd in the EST time zone. In this example, the date
         * column would hold the date representing September 23rd at midnight in GMT time.
         * (1474588800000)
         *
         * The reason we store GMT time and not local time is because it is best practice to have a
         * "normalized", or standard when storing the date and adjust as necessary when
         * displaying the date. Normalizing the date also allows us an easy way to convert to
         * local time at midnight, as all we have to do is add a particular time zone's GMT
         * offset to this date to get local time at midnight on the appropriate date.
         */
        const val COLUMN_DATE = "date"

        /* Weather ID as returned by API, used to identify the icon to be used */
        const val COLUMN_WEATHER_ID = "weather_id"

        /* Min and max temperatures in °C for the day (stored as floats in the database) */
        const val COLUMN_MIN_TEMP = "min"
        const val COLUMN_MAX_TEMP = "max"

        /* Humidity is stored as a float representing percentage */
        const val COLUMN_HUMIDITY = "humidity"

        /* Pressure is stored as a float representing percentage */
        const val COLUMN_PRESSURE = "pressure"

        /* Wind speed is stored as a float representing wind speed in mph */
        const val COLUMN_WIND_SPEED = "wind"

        /*
     * Degrees are meteorological degrees (e.g, 0 is north, 180 is south).
     * Stored as floats in the database.
     *
     * Note: These degrees are not to be confused with temperature degrees of the weather.
     */
        const val COLUMN_DEGREES = "degrees"
    }
}