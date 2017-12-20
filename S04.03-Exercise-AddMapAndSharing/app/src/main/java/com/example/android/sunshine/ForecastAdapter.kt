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

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import com.example.android.sunshine.R.layout.forecast_list_item
import kotlinx.android.synthetic.main.forecast_list_item.view.*

/**
 * [ForecastAdapter] exposes a list of weather forecasts to a
 * [android.support.v7.widget.RecyclerView]
 *
 * @param mClickHandler An on-click handler that we've defined to make it easy for an Activity to
 * interface with our RecyclerView
 */
class ForecastAdapter(private val mClickHandler: ForecastAdapterOnClickHandler) :
        RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder>() {

    private var mWeatherData: Array<String> = emptyArray()

    /**
     * The interface that receives onClick messages.
     */
    interface ForecastAdapterOnClickHandler {
        fun onClick(weatherForDay: String)
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    inner class ForecastAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {
        val mWeatherTextView: TextView

        init {
            mWeatherTextView = view.tv_weather_data
            view.setOnClickListener(this)
        }

        /**
         * This gets called by the child views during a click.
         *
         * @param v The View that was clicked
         */
        override fun onClick(v: View) {
            val weatherForDay = mWeatherData[adapterPosition]
            mClickHandler.onClick(weatherForDay)
        }
    }

    /**
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (which ours doesn't) you
     * can use this viewType integer to provide a different layout. See
     * [android.support.v7.widget.RecyclerView.Adapter.getItemViewType]
     * for more details.
     * @return A new ForecastAdapterViewHolder that holds the View for each list item
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ForecastAdapterViewHolder {
        val context = viewGroup.context
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false

        val view = inflater.inflate(forecast_list_item, viewGroup, shouldAttachToParentImmediately)
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
        val weatherForThisDay = mWeatherData[position]
        forecastAdapterViewHolder.mWeatherTextView.text = weatherForThisDay
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    override fun getItemCount(): Int {
        return mWeatherData.size
    }

    /**
     * This method is used to set the weather forecast on a ForecastAdapter if we've already
     * created one. This is handy when we get new data from the web but don't want to create a
     * new ForecastAdapter to display it.
     *
     * @param weatherData The new weather data to be displayed.
     */
    fun setWeatherData(weatherData: Array<String>) {
        mWeatherData = weatherData
        notifyDataSetChanged()
    }
}