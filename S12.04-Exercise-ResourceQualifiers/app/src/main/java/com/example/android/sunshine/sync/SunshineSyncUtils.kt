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
package com.example.android.sunshine.sync

import android.content.Context
import android.content.Intent
import com.example.android.sunshine.data.WeatherContract
import com.example.android.sunshine.utilities.TriggerConfigurator
import com.firebase.jobdispatcher.*
import java.util.concurrent.TimeUnit


/*
 * Interval at which to sync with the weather. Use TimeUnit for convenience, rather than
 * writing out a bunch of multiplication ourselves and risk making a silly mistake.
 */
private const val SYNC_INTERVAL_HOURS = 3
private val SYNC_INTERVAL_SECONDS = TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS.toLong()).toInt()
private val SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 3

private var sInitialized: Boolean = false

private const val SUNSHINE_SYNC_TAG = "sunshine-sync"

/**
 * Schedules a repeating sync of Sunshine's weather data using FirebaseJobDispatcher.
 * @param context Context used to create the GooglePlayDriver that powers the
 * FirebaseJobDispatcher
 */
private fun scheduleFirebaseJobDispatcherSync(context: Context) {

    val driver = GooglePlayDriver(context)
    val dispatcher = FirebaseJobDispatcher(driver)

    /* Create the Job to periodically sync Sunshine */
    val syncSunshineJob = TriggerConfigurator.executionWindow(
            dispatcher.newJobBuilder()
                    /* The Service that will be used to sync Sunshine's data */
                    .setService(SunshineFirebaseJobService::class.java)
                    /* Set the UNIQUE tag used to identify this Job */
                    .setTag(SUNSHINE_SYNC_TAG)
                    /*
                     * Network constraints on which this Job should run. We choose to run on any
                     * network, but you can also choose to run only on un-metered networks or when the
                     * device is charging. It might be a good idea to include a preference for this,
                     * as some users may not want to download any data on their mobile plan. ($$$)
                     */
                    .setConstraints(Constraint.ON_ANY_NETWORK)
                    /*
                     * setLifetime sets how long this job should persist. The options are to keep the
                     * Job "forever" or to have it die the next time the device boots up.
                     */
                    .setLifetime(Lifetime.FOREVER)
                    /*
                     * We want Sunshine's weather data to stay up to date, so we tell this Job to recur.
                     */
                    .setRecurring(true),
            SYNC_INTERVAL_SECONDS,
            SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS)
            /*
             * If a Job with the tag with provided already exists, this new job will replace
             * the old one.
             */
            .setReplaceCurrent(true)
            /* Once the Job is ready, call the builder's build method to return the Job */
            .build()

    /* Schedule the Job with the dispatcher */
    dispatcher.schedule(syncSunshineJob)
}

/**
 * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
 * immediate sync is required, this method will take care of making sure that sync occurs.
 *
 * @param context Context that will be passed to other methods and used to access the
 * ContentResolver
 */
@Synchronized
fun initialize(context: Context) {

    /*
     * Only perform initialization once per app lifetime. If initialization has already been
     * performed, we have nothing to do in this method.
     */
    if (sInitialized) return

    sInitialized = true

    /*
     * This method call triggers Sunshine to create its task to synchronize weather data
     * periodically.
     */
    scheduleFirebaseJobDispatcherSync(context)

    /*
     * We need to check to see if our ContentProvider has data to display in our forecast
     * list. However, performing a query on the main thread is a bad idea as this may
     * cause our UI to lag. Therefore, we create a thread in which we will run the query
     * to check the contents of our ContentProvider.
     */
    val checkForEmpty = Thread(Runnable {
        /* URI for every row of weather data in our weather table*/
        val forecastQueryUri = WeatherContract.CONTENT_URI

        /*
             * Since this query is going to be used only as a check to see if we have any
             * data (rather than to display data), we just need to PROJECT the ID of each
             * row. In our queries where we display data, we need to PROJECT more columns
             * to determine what weather details need to be displayed.
             */
        val projectionColumns = arrayOf(WeatherContract.WeatherEntry._ID)
        val selectionStatement = WeatherContract.Companion.sqlSelectForTodayOnwards

        /* Here, we perform the query to check to see if we have any weather data */
        val cursor = context.contentResolver.query(
                forecastQueryUri,
                projectionColumns,
                selectionStatement, null, null)
        /*
             * A Cursor object can be null for various different reasons. A few are
             * listed below.
             *
             *   1) Invalid URI
             *   2) A certain ContentProvider's query method returns null
             *   3) A RemoteException was thrown.
             *
             * Bottom line, it is generally a good idea to check if a Cursor returned
             * from a ContentResolver is null.
             *
             * If the Cursor was null OR if it was empty, we need to sync immediately to
             * be able to display data to the user.
             */
        if (null == cursor || cursor.count == 0) {
            startImmediateSync(context)
        }

        /* Make sure to close the Cursor to avoid memory leaks! */
        cursor!!.close()
    })

    /* Finally, once the thread is prepared, fire it off to perform our checks. */
    checkForEmpty.start()
}

/**
 * Helper method to perform a sync immediately using an IntentService for asynchronous
 * execution.
 *
 * @param context The Context used to start the IntentService for the sync.
 */
fun startImmediateSync(context: Context) {
    val intentToSyncImmediately = Intent(context, SunshineSyncIntentService::class.java)
    context.startService(intentToSyncImmediately)
}
