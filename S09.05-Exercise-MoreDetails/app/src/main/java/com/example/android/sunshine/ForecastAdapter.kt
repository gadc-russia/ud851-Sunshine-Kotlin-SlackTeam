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

import android.content.Context
import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.android.sunshine.utilities.formatHighLows
import com.example.android.sunshine.utilities.getFriendlyDateString
import com.example.android.sunshine.utilities.getStringForWeatherCondition
import kotlinx.android.synthetic.main.forecast_list_item.view.*

/**
 * [ForecastAdapter] exposes a list of weather forecasts
 * from a [android.database.Cursor] to a [android.support.v7.widget.RecyclerView].
 *
 * @param context Used to talk to the UI and app resources
 * @param clickHandler The on-click handler for this adapter. This single handler is called
 * when an item is clicked.
 */
/* The context we use to utility methods, app resources and layout inflaters */
/*
 * Below, we've defined an interface to handle clicks on items within this Adapter. In the
 * constructor of our ForecastAdapter, we receive an instance of a class that has implemented
 * said interface. We store that instance in this variable to call the onClick method whenever
 * an item is clicked in the list.
 */
internal class ForecastAdapter(
        private val mContext: Context,
        private val mClickHandler: ForecastAdapterOnClickHandler) :
        RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder>() {

    private var mCursor: Cursor? = null

    /**
     * The interface that receives onClick messages.
     */
    interface ForecastAdapterOnClickHandler {
        //      TODO (36) Refactor onClick to accept a long as its parameter rather than a String
        fun onClick(weatherForDay: String)
    }

    /**
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (like ours does) you
     * can use this viewType integer to provide a different layout. See
     * [android.support.v7.widget.RecyclerView.Adapter.getItemViewType]
     * for more details.
     * @return A new ForecastAdapterViewHolder that holds the View for each list item
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ForecastAdapterViewHolder {

        val view = LayoutInflater
                .from(mContext)
                .inflate(R.layout.forecast_list_item, viewGroup, false)

        view.isFocusable = true

        return ForecastAdapterViewHolder(view)
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the weather
     * details for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param forecastAdapterViewHolder The ViewHolder which should be updated to represent the
     * contents of the item at the given position in the data set.
     * @param position                  The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(forecastAdapterViewHolder: ForecastAdapterViewHolder, position: Int) {
        mCursor!!.moveToPosition(position)


        /*******************
         * Weather Summary *
         */
        /* Read date from the cursor */
        val dateInMillis = mCursor!!.getLong(MainActivity.INDEX_WEATHER_DATE)
        /* Get human readable string using our utility method */
        val dateString = getFriendlyDateString(mContext, dateInMillis, false)
        /* Use the weatherId to obtain the proper description */
        val weatherId = mCursor!!.getInt(MainActivity.INDEX_WEATHER_CONDITION_ID)
        val description = getStringForWeatherCondition(mContext, weatherId)
        /* Read high temperature from the cursor (in degrees celsius) */
        val highInCelsius = mCursor!!.getDouble(MainActivity.INDEX_WEATHER_MAX_TEMP)
        /* Read low temperature from the cursor (in degrees celsius) */
        val lowInCelsius = mCursor!!.getDouble(MainActivity.INDEX_WEATHER_MIN_TEMP)

        val highAndLowTemperature = formatHighLows(mContext, highInCelsius, lowInCelsius)

        val weatherSummary = dateString + " - " + description + " - " + highAndLowTemperature

        forecastAdapterViewHolder.weatherSummary.setText(weatherSummary)
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    override fun getItemCount(): Int {
        return if (null == mCursor) 0 else mCursor!!.count
    }

    /**
     * Swaps the cursor used by the ForecastAdapter for its weather data. This method is called by
     * MainActivity after a load has finished, as well as when the Loader responsible for loading
     * the weather data is reset. When this method is called, we assume we have a completely new
     * set of data, so we call notifyDataSetChanged to tell the RecyclerView to update.
     *
     * @param newCursor the new cursor to use as ForecastAdapter's data source
     */
    fun swapCursor(newCursor: Cursor) {
        mCursor = newCursor
        notifyDataSetChanged()
    }

    /**
     * A ViewHolder is a required part of the pattern for RecyclerViews. It mostly behaves as
     * a cache of the child views for a forecast item. It's also a convenient place to set an
     * OnClickListener, since it has access to the adapter and the views.
     */
    internal inner class ForecastAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val weatherSummary: TextView

        init {

            weatherSummary = view.tv_weather_data
            view.setOnClickListener(this)
        }

        /**
         * This gets called by the child views during a click. We fetch the date that has been
         * selected, and then call the onClick handler registered with this adapter, passing that
         * date.
         *
         * @param v the View that was clicked
         */
        override fun onClick(v: View) {
            //          TODO (37) Instead of passing the String for the clicked item, pass the date from the cursor
            val weatherForDay = weatherSummary.text.toString()
            mClickHandler.onClick(weatherForDay)
        }
    }
}