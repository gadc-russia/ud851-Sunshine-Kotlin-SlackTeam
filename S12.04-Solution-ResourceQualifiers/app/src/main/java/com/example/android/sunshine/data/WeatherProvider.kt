/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.TargetApi
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.example.android.sunshine.utilities.isDateNormalized

/**
 * This class serves as the ContentProvider for all of Sunshine's data. This class allows us to
 * bulkInsert data, query data, and delete data.
 *
 *
 * Although ContentProvider implementation requires the implementation of additional methods to
 * perform single inserts, updates, and the ability to get the type of the data from a URI.
 * However, here, they are not implemented for the sake of brevity and simplicity. If you would
 * like, you may implement them on your own. However, we are not going to be teaching how to do
 * so in this course.
 */
class WeatherProvider : ContentProvider() {
    private lateinit var mOpenHelper: WeatherDbHelper

    /**
     * In onCreate, we initialize our content provider on startup. This method is called for all
     * registered content providers on the application main thread at application launch time.
     * It must not perform lengthy operations, or application startup will be delayed.
     *
     * Nontrivial initialization (such as opening, upgrading, and scanning
     * databases) should be deferred until the content provider is used (via [.query],
     * [.bulkInsert], etc).
     *
     * Deferred initialization keeps application startup fast, avoids unnecessary work if the
     * provider turns out not to be needed, and stops database errors (such as a full disk) from
     * halting application launch.
     *
     * @return true if the provider was successfully loaded, false otherwise
     */
    override fun onCreate(): Boolean {
        /*
         * As noted in the comment above, onCreate is run on the main thread, so performing any
         * lengthy operations will cause lag in your app. Since WeatherDbHelper's constructor is
         * very lightweight, we are safe to perform that initialization here.
         */
        mOpenHelper = WeatherDbHelper(context)
        return true
    }

    /**
     * Handles requests to insert a set of new rows. In Sunshine, we are only going to be
     * inserting multiple rows of data at a time from a weather forecast. There is no use case
     * for inserting a single row of data into our ContentProvider, and so we are only going to
     * implement bulkInsert. In a normal ContentProvider's implementation, you will probably want
     * to provide proper functionality for the insert method as well.
     *
     * @param uri    The content:// URI of the insertion request.
     * @param values An array of sets of column_name/value pairs to add to the database.
     * This must not be `null`.
     *
     * @return The number of values that were inserted.
     */
    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val db = mOpenHelper.writableDatabase

        when (sUriMatcher.match(uri)) {

            CODE_WEATHER -> {
                db.beginTransaction()
                var rowsInserted = 0
                try {
                    for (value in values) {
                        val weatherDate = value.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE)!!
                        if (!isDateNormalized(weatherDate)) {
                            throw IllegalArgumentException("Date must be normalized to insert")
                        }

                        val _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value)
                        if (_id != -1L) {
                            rowsInserted++
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                if (rowsInserted > 0) {
                    context.contentResolver.notifyChange(uri, null)
                }
                return rowsInserted
            }
            else -> return super.bulkInsert(uri, values)
        }
    }

    /**
     * Handles query requests from clients. We will use this method in Sunshine to query for all
     * of our weather data as well as to query for the weather on a particular day.
     *
     * @param uri           The URI to query
     * @param projection    The list of columns to put into the cursor. If null, all columns are
     * included.
     * @param selection     A selection criteria to apply when filtering rows. If null, then all
     * rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     * the values from selectionArgs, in order that they appear in the
     * selection.
     * @param sortOrder     How the rows in the cursor should be sorted.
     * @return A Cursor containing the results of the query. In our implementation,
     */
    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {

        val cursor: Cursor

        /*
         * Here's the switch statement that, given a URI, will determine what kind of request is
         * being made and query the database accordingly.
         */
        when (sUriMatcher.match(uri)) {

        /*
             * When sUriMatcher's match method is called with a URI that looks something like this
             *
             *      content://com.example.android.sunshine/weather/1472214172
             *
             * sUriMatcher's match method will return the code that indicates to us that we need
             * to return the weather for a particular date. The date in this code is encoded in
             * milliseconds and is at the very end of the URI (1472214172) and can be accessed
             * programmatically using Uri's getLastPathSegment method.
             *
             * In this case, we want to return a cursor that contains one row of weather data for
             * a particular date.
             */
            CODE_WEATHER_WITH_DATE -> {

                /*
                 * In order to determine the date associated with this URI, we look at the last
                 * path segment. In the comment above, the last path segment is 1472214172 and
                 * represents the number of seconds since the epoch, or UTC time.
                 */
                val normalizedUtcDateString = uri.lastPathSegment

                /*
                 * The query method accepts a string array of arguments, as there may be more
                 * than one "?" in the selection statement. Even though in our case, we only have
                 * one "?", we have to create a string array that only contains one element
                 * because this method signature accepts a string array.
                 */
                val selectionArguments = arrayOf(normalizedUtcDateString)

                cursor = mOpenHelper.readableDatabase.query(
                        /* Table we are going to query */
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        /*
                         * A projection designates the columns we want returned in our Cursor.
                         * Passing null will return all columns of data within the Cursor.
                         * However, if you don't need all the data from the table, it's best
                         * practice to limit the columns returned in the Cursor with a projection.
                         */
                        projection,
                        /*
                         * The URI that matches CODE_WEATHER_WITH_DATE contains a date at the end
                         * of it. We extract that date and use it with these next two lines to
                         * specify the row of weather we want returned in the cursor. We use a
                         * question mark here and then designate selectionArguments as the next
                         * argument for performance reasons. Whatever Strings are contained
                         * within the selectionArguments array will be inserted into the
                         * selection statement by SQLite under the hood.
                         */
                        WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ",
                        selectionArguments, null, null,
                        sortOrder)
            }

        /*
             * When sUriMatcher's match method is called with a URI that looks EXACTLY like this
             *
             *      content://com.example.android.sunshine/weather/
             *
             * sUriMatcher's match method will return the code that indicates to us that we need
             * to return all of the weather in our weather table.
             *
             * In this case, we want to return a cursor that contains every row of weather data
             * in our weather table.
             */
            CODE_WEATHER -> {
                cursor = mOpenHelper.readableDatabase.query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs, null, null,
                        sortOrder)
            }

            else -> throw UnsupportedOperationException("Unknown uri: " + uri)
        }

        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    /**
     * Deletes data at a given URI with optional arguments for more fine tuned deletions.
     *
     * @param uri           The full URI to query
     * @param selection     An optional restriction to apply to rows when deleting.
     * @param selectionArgs Used in conjunction with the selection statement
     * @return The number of rows deleted
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var insedeSelection = selection

        /* Users of the delete method will expect the number of rows deleted to be returned. */
        val numRowsDeleted: Int

        /*
         * If we pass null as the selection to SQLiteDatabase#delete, our entire table will be
         * deleted. However, if we do pass null and delete all of the rows in the table, we won't
         * know how many rows were deleted. According to the documentation for SQLiteDatabase,
         * passing "1" for the selection will delete all rows and return the number of rows
         * deleted, which is what the caller of this method expects.
         */
        if (null == selection) insedeSelection = "1"

        when (sUriMatcher.match(uri)) {

            CODE_WEATHER -> numRowsDeleted = mOpenHelper.writableDatabase.delete(
                    WeatherContract.WeatherEntry.TABLE_NAME,
                    insedeSelection,
                    selectionArgs)

            else -> throw UnsupportedOperationException("Unknown uri: " + uri)
        }

        /* If we actually deleted any rows, notify that a change has occurred to this URI */
        if (numRowsDeleted != 0) {
            context.contentResolver.notifyChange(uri, null)
        }

        return numRowsDeleted
    }

    /**
     * In Sunshine, we aren't going to do anything with this method. However, we are required to
     * override it as WeatherProvider extends ContentProvider and getType is an abstract method in
     * ContentProvider. Normally, this method handles requests for the MIME type of the data at the
     * given URI. For example, if your app provided images at a particular URI, then you would
     * return an image URI from this method.
     *
     * @param uri the URI to query.
     * @return nothing in Sunshine, but normally a MIME type string, or null if there is no type.
     */
    override fun getType(uri: Uri): String? {
        throw RuntimeException("We are not implementing getType in Sunshine.")
    }

    /**
     * In Sunshine, we aren't going to do anything with this method. However, we are required to
     * override it as WeatherProvider extends ContentProvider and insert is an abstract method in
     * ContentProvider. Rather than the single insert method, we are only going to implement
     * [WeatherProvider.bulkInsert].
     *
     * @param uri    The URI of the insertion request. This must not be null.
     * @param values A set of column_name/value pairs to add to the database.
     * This must not be null
     * @return nothing in Sunshine, but normally the URI for the newly inserted item.
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw RuntimeException(
                "We are not implementing insert in Sunshine. Use bulkInsert instead")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        throw RuntimeException("We are not implementing update in Sunshine")
    }

    /**
     * You do not need to call this method. This is a method specifically to assist the testing
     * framework in running smoothly. You can read more at:
     * http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
     */
    @TargetApi(11)
    override fun shutdown() {
        mOpenHelper.close()
        super.shutdown()
    }

    companion object {

        /*
         * These constant will be used to match URIs with the data they are looking for. We will take
         * advantage of the UriMatcher class to make that matching MUCH easier than doing something
         * ourselves, such as using regular expressions.
         */
        val CODE_WEATHER = 100
        val CODE_WEATHER_WITH_DATE = 101

        /*
         * The URI Matcher used by this content provider. The leading "s" in this variable name
         * signifies that this UriMatcher is a static member variable of WeatherProvider and is a
         * common convention in Android programming.
         */
        private val sUriMatcher = buildUriMatcher()

        /**
         * Creates the UriMatcher that will match each URI to the CODE_WEATHER and
         * CODE_WEATHER_WITH_DATE constants defined above.
         *
         *
         * It's possible you might be thinking, "Why create a UriMatcher when you can use regular
         * expressions instead? After all, we really just need to match some patterns, and we can
         * use regular expressions to do that right?" Because you're not crazy, that's why.
         *
         *
         * UriMatcher does all the hard work for you. You just have to tell it which code to match
         * with which URI, and it does the rest automagically. Remember, the best programmers try
         * to never reinvent the wheel. If there is a solution for a problem that exists and has
         * been tested and proven, you should almost always use it unless there is a compelling
         * reason not to.
         *
         * @return A UriMatcher that correctly matches the constants for CODE_WEATHER and CODE_WEATHER_WITH_DATE
         */
        fun buildUriMatcher(): UriMatcher {

            /*
             * All paths added to the UriMatcher have a corresponding code to return when a match is
             * found. The code passed into the constructor of UriMatcher here represents the code to
             * return for the root URI. It's common to use NO_MATCH as the code for this case.
             */
            val matcher = UriMatcher(UriMatcher.NO_MATCH)
            val authority = WeatherContract.CONTENT_AUTHORITY

            /*
             * For each type of URI you want to add, create a corresponding code. Preferably, these are
             * constant fields in your class so that you can use them throughout the class and you no
             * they aren't going to change. In Sunshine, we use CODE_WEATHER or CODE_WEATHER_WITH_DATE.
             */

            /* This URI is content://com.example.android.sunshine/weather/ */
            matcher.addURI(authority, WeatherContract.PATH_WEATHER, CODE_WEATHER)

            /*
             * This URI would look something like content://com.example.android.sunshine/weather/1472214172
             * The "/#" signifies to the UriMatcher that if PATH_WEATHER is followed by ANY number,
             * that it should return the CODE_WEATHER_WITH_DATE code
             */
            matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/#", CODE_WEATHER_WITH_DATE)

            return matcher
        }
    }
}