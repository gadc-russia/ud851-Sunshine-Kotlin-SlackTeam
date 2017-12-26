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

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.example.android.sunshine.data.WeatherContract
import com.example.android.sunshine.data.getLocationCoordinates
import com.example.android.sunshine.utilities.insertFakeData
import kotlinx.android.synthetic.main.activity_forecast.*

class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>,
        ForecastAdapter.ForecastAdapterOnClickHandler {

    private val TAG = MainActivity::class.java.simpleName
    private val FORECAST_LOADER_ID = 0
    private var PREFERENCES_HAVE_BEEN_UPDATED = false

    /*
    * The columns of data that we are interested in displaying within our MainActivity's list of
    * weather data.
    */
    val MAIN_FORECAST_PROJECTION = arrayOf(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, WeatherContract.WeatherEntry.COLUMN_WEATHER_ID)

    /*
     * We store the indices of the values in the array of Strings above to more quickly be able to
     * access the data from our query. If the order of the Strings above changes, these indices
     * must be adjusted to match the order of the Strings.
     */
    companion object {
        const val INDEX_WEATHER_DATE = 0
        const val INDEX_WEATHER_MAX_TEMP = 1
        const val INDEX_WEATHER_MIN_TEMP = 2
        const val INDEX_WEATHER_CONDITION_ID = 3
    }

    /*
 * This ID will be used to identify the Loader responsible for loading our weather forecast. In
 * some cases, one Activity can deal with many Loaders. However, in our case, there is only one.
 * We will still use this ID to initialize the loader and create the loader for best practice.
 * Please note that 44 was chosen arbitrarily. You can use whatever number you like, so long as
 * it is unique and consistent.
 */
    private val ID_FORECAST_LOADER = 44

    private lateinit var mForecastAdapter: ForecastAdapter
    private var mPosition = RecyclerView.NO_POSITION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)
        supportActionBar!!.elevation = 0f

        insertFakeData(this)

        /*
         * A LinearLayoutManager is responsible for measuring and positioning item views within a
         * RecyclerView into a linear list. This means that it can produce either a horizontal or
         * vertical list depending on which parameter you pass in to the LinearLayoutManager
         * constructor. In our case, we want a vertical list, so we pass in the constant from the
         * LinearLayoutManager class for vertical lists, LinearLayoutManager.VERTICAL.
         *
         * There are other LayoutManagers available to display your data in uniform grids,
         * staggered grids, and more! See the developer documentation for more details.
         *
         * The third parameter (shouldReverseLayout) should be true if you want to reverse your
         * layout. Generally, this is only true with horizontal lists that need to support a
         * right-to-left layout.
         */
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        /* setLayoutManager associates the LayoutManager we created above with our RecyclerView */
        recyclerview_forecast.layoutManager = layoutManager

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        recyclerview_forecast.setHasFixedSize(true)

        /*
         * The ForecastAdapter is responsible for linking our weather data with the Views that
         * will end up displaying our weather data.
         *
         * Although passing in "this" twice may seem strange, it is actually a sign of separation
         * of concerns, which is best programming practice. The ForecastAdapter requires an
         * Android Context (which all Activities are) as well as an onClickHandler. Since our
         * MainActivity implements the ForecastAdapter ForecastOnClickHandler interface, "this"
         * is also an instance of that type of handler.
         */
        mForecastAdapter = ForecastAdapter(this, this)

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        recyclerview_forecast.adapter = mForecastAdapter

        showLoading()

        /*
         * Ensures a loader is initialized and active. If the loader doesn't already exist, one is
         * created and (if the activity/fragment is currently started) starts the loader. Otherwise
         * the last created loader is re-used.
         */
        supportLoaderManager.initLoader(ID_FORECAST_LOADER, Bundle.EMPTY, this)
    }

    /**
     * Uses the URI scheme for showing a location found on a map in conjunction with
     * an implicit Intent. This super-handy Intent is detailed in the "Common Intents" page of
     * Android's developer site:
     *
     * @see "http://developer.android.com/guide/components/intents-common.html.Maps"
     *
     *
     * Protip: Hold Command on Mac or Control on Windows and click that link to automagically
     * open the Common Intents page
     */
    private fun openPreferredLocationInMap() {
        val coords = getLocationCoordinates(this)
        val posLat = java.lang.Double.toString(coords[0])
        val posLong = java.lang.Double.toString(coords[1])
        val geoLocation = Uri.parse("geo:$posLat,$posLong")

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = geoLocation

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!")
        }
    }

    /**
     * Called by the [android.support.v4.app.LoaderManagerImpl] when a new Loader needs to be
     * created. This Activity only uses one loader, so we don't necessarily NEED to check the
     * loaderId, but this is certainly best practice.
     *
     * @param loaderId The loader ID for which we need to create a loader
     * @param bundle   Any arguments supplied by the caller
     * @return A new Loader instance that is ready to start loading.
     */
    override fun onCreateLoader(loaderId: Int, bundle: Bundle): Loader<Cursor> {


        when (loaderId) {

            ID_FORECAST_LOADER -> {
                /* URI for all rows of weather data in our weather table */
                val forecastQueryUri = WeatherContract.CONTENT_URI
                /* Sort order: Ascending by date */
                val sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"
                /*
                 * A SELECTION in SQL declares which rows you'd like to return. In our case, we
                 * want all weather data from today onwards that is stored in our weather table.
                 * We created a handy method to do that in our WeatherEntry class.
                 */
                val selection = WeatherContract.sqlSelectForTodayOnwards

                return CursorLoader(this,
                        forecastQueryUri,
                        MAIN_FORECAST_PROJECTION,
                        selection, null,
                        sortOrder)
            }

            else -> throw RuntimeException("Loader Not Implemented: " + loaderId)
        }
    }

    /**
     * Called when a Loader has finished loading its data.
     *
     * NOTE: There is one small bug in this code. If no data is present in the cursor do to an
     * initial load being performed with no access to internet, the loading indicator will show
     * indefinitely, until data is present from the ContentProvider. This will be fixed in a
     * future version of the course.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {


        mForecastAdapter.swapCursor(data)
        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0
        recyclerview_forecast.smoothScrollToPosition(mPosition)
        if (data.count != 0) showWeatherDataView()
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    override fun onLoaderReset(loader: Loader<Cursor>) {
        /*
         * Since this Loader's data is now invalid, we need to clear the Adapter that is
         * displaying the data.
         */
        mForecastAdapter.swapCursor(null!!)
    }

    //  TODO (38) Refactor onClick to accept a long instead of a String as its parameter
    /**
     * This method is for responding to clicks from our list.
     *
     * @param weatherForDay String describing weather details for a particular day
     */
    override fun onClick(weatherForDay: String) {
        //      TODO (39) Refactor onClick to build a URI for the clicked date and and pass it with the Intent using setData
        val context = this
        val destinationClass = DetailActivity::class.java
        val intentToStartDetailActivity = Intent(context, destinationClass)
        intentToStartDetailActivity.putExtra(Intent.EXTRA_TEXT, weatherForDay)
        startActivity(intentToStartDetailActivity)
    }

    /**
     * This method will make the View for the weather data visible and hide the error message and
     * loading indicator.
     *
     *
     * Since it is okay to redundantly set the visibility of a View, we don't need to check whether
     * each view is currently visible or invisible.
     */
    private fun showWeatherDataView() {
        /* First, hide the loading indicator */
        pb_loading_indicator.visibility = View.INVISIBLE
        /* Finally, make sure the weather data is visible */
        recyclerview_forecast.visibility = View.VISIBLE
    }

    /**
     * This method will make the loading indicator visible and hide the weather View and error
     * message.
     *
     *
     * Since it is okay to redundantly set the visibility of a View, we don't need to check whether
     * each view is currently visible or invisible.
     */
    private fun showLoading() {
        /* Then, hide the weather data */
        recyclerview_forecast.visibility = View.INVISIBLE
        /* Finally, show the loading indicator */
        pb_loading_indicator.visibility = View.VISIBLE
    }

    /**
     * This is where we inflate and set up the menu for this Activity.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     *
     * @see .onPrepareOptionsMenu
     *
     * @see .onOptionsItemSelected
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        menuInflater.inflate(R.menu.forecast, menu)
        /* Return true so that the menu is displayed in the Toolbar */
        return true
    }

    /**
     * Callback invoked when a menu item was selected from this Activity's menu.
     *
     * @param item The menu item that was selected by the user
     *
     * @return true if you handle the menu click here, false otherwise
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId

        if (id == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        if (id == R.id.action_map) {
            openPreferredLocationInMap()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
