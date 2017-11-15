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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.example.android.sunshine.data.TestUtilities.getStaticIntegerField
import com.example.android.sunshine.data.TestUtilities.getStaticStringField
import com.example.android.sunshine.data.TestUtilities.studentReadableClassNotFound
import com.example.android.sunshine.data.TestUtilities.studentReadableNoSuchField
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Used to test the database we use in Sunshine to cache weather data. Within these tests, we
 * test the following:
 *
 *
 *
 *
 * 1) Creation of the database with proper table(s)
 * 2) Insertion of single record into our weather table
 * 3) When a record is already stored in the weather table with a particular date, a new record
 * with the same date will overwrite that record.
 * 4) Verify that NON NULL constraints are working properly on record inserts
 * 5) Verify auto increment is working with the ID
 * 6) Test the onUpgrade functionality of the WeatherDbHelper
 */
@RunWith(AndroidJUnit4::class)
class TestSunshineDatabase {

    /*
     * Context used to perform operations on the database and create WeatherDbHelpers.
     */
    private val context = InstrumentationRegistry.getTargetContext()

    private lateinit var weatherEntryClass: Class<*>
    private lateinit var weatherDbHelperClass: Class<*>

    private lateinit var database: SQLiteDatabase
    private lateinit var dbHelper: SQLiteOpenHelper

    @Before
    fun before() {
        try {
            weatherEntryClass = Class.forName(dataPackageName + weatherEntryName)
            if (!BaseColumns::class.java.isAssignableFrom(weatherEntryClass)) {
                val weatherEntryDoesNotImplementBaseColumns = "WeatherEntry class needs to " + "implement the interface BaseColumns, but does not."
                fail(weatherEntryDoesNotImplementBaseColumns)
            }

            REFLECTED_TABLE_NAME = getStaticStringField(weatherEntryClass, tableNameVariableName)
            REFLECTED_COLUMN_DATE = getStaticStringField(weatherEntryClass, columnDateVariableName)
            REFLECTED_COLUMN_WEATHER_ID = getStaticStringField(weatherEntryClass, columnWeatherIdVariableName)
            REFLECTED_COLUMN_MIN = getStaticStringField(weatherEntryClass, columnMinVariableName)
            REFLECTED_COLUMN_MAX = getStaticStringField(weatherEntryClass, columnMaxVariableName)
            REFLECTED_COLUMN_HUMIDITY = getStaticStringField(weatherEntryClass, columnHumidityVariableName)
            REFLECTED_COLUMN_PRESSURE = getStaticStringField(weatherEntryClass, columnPressureVariableName)
            REFLECTED_COLUMN_WIND_SPEED = getStaticStringField(weatherEntryClass, columnWindSpeedVariableName)
            REFLECTED_COLUMN_WIND_DIR = getStaticStringField(weatherEntryClass, columnWindDirVariableName)

            weatherDbHelperClass = Class.forName(dataPackageName + weatherDbHelperName)
            val weatherDbHelperSuperclass = weatherDbHelperClass.superclass

            /* Sort order to return in Cursor */
            if (weatherDbHelperSuperclass == null || weatherDbHelperSuperclass == Any::class.java) {
                val noExplicitSuperclass = "WeatherDbHelper needs to extend SQLiteOpenHelper, but yours currently doesn't extend a class at all."
                fail(noExplicitSuperclass)
            } else if (weatherDbHelperSuperclass != null) {
                val weatherDbHelperSuperclassName = weatherDbHelperSuperclass.simpleName
                val doesNotExtendOpenHelper = "WeatherDbHelper needs to extend SQLiteOpenHelper but yours extends " + weatherDbHelperSuperclassName

                assertTrue(doesNotExtendOpenHelper, SQLiteOpenHelper::class.java.isAssignableFrom(weatherDbHelperSuperclass))
            }

            REFLECTED_DATABASE_NAME = getStaticStringField(
                    weatherDbHelperClass, databaseNameVariableName)

            REFLECTED_DATABASE_VERSION = getStaticIntegerField(
                    weatherDbHelperClass, databaseVersionVariableName)!!

            val expectedDatabaseVersion = 1
            val databaseVersionShouldBe1 = ("Database version should be "
                    + expectedDatabaseVersion + " but isn't.")

            assertEquals(databaseVersionShouldBe1,
                    expectedDatabaseVersion,
                    REFLECTED_DATABASE_VERSION)

            val weatherDbHelperCtor = weatherDbHelperClass.getConstructor(Context::class.java)
            dbHelper = weatherDbHelperCtor.newInstance(context) as SQLiteOpenHelper
            context.deleteDatabase(REFLECTED_DATABASE_NAME)

            val getWritableDatabase = SQLiteOpenHelper::class.java.getDeclaredMethod("getWritableDatabase")
            database = getWritableDatabase.invoke(dbHelper) as SQLiteDatabase

        } catch (e: ClassNotFoundException) {
            fail(studentReadableClassNotFound(e))
        } catch (e: NoSuchFieldException) {
            fail(studentReadableNoSuchField(e))
        } catch (e: IllegalAccessException) {
            fail(e.message)
        } catch (e: NoSuchMethodException) {
            fail(e.message)
        } catch (e: InstantiationException) {
            fail(e.message)
        } catch (e: InvocationTargetException) {
            fail(e.message)
        }

    }

    /**
     * Tests to ensure that inserts into your database results in automatically incrementing row
     * IDs and that row IDs are not reused.
     *
     *
     * If the INTEGER PRIMARY KEY column is not explicitly given a value, then it will be filled
     * automatically with an unused integer, usually one more than the largest _ID currently in
     * use. This is true regardless of whether or not the AUTOINCREMENT keyword is used.
     *
     *
     * If the AUTOINCREMENT keyword appears after INTEGER PRIMARY KEY, that changes the automatic
     * _ID assignment algorithm to prevent the reuse of _IDs over the lifetime of the database.
     * In other words, the purpose of AUTOINCREMENT is to prevent the reuse of _IDs from previously
     * deleted rows.
     *
     *
     * To test this, we first insert a row into the database and get its _ID. Then, we'll delete
     * that row, change the data that we're going to insert, and insert the changed data into the
     * database again. If AUTOINCREMENT isn't set up properly in the WeatherDbHelper's table
     * create statement, then the _ID of the first insert will be reused. However, if AUTOINCREMENT
     * is setup properly, that older ID will NOT be reused, and the test will pass.
     */
    @Test
    fun testIntegerAutoincrement() {

        /* First, let's ensure we have some values in our table initially */
        testInsertSingleRecordIntoWeatherTable()

        /* Obtain weather values from TestUtilities */
        val testWeatherValues = TestUtilities.createTestWeatherContentValues()

        /* Get the date of the testWeatherValues to ensure we use a different date later */
        val originalDate = testWeatherValues.getAsLong(REFLECTED_COLUMN_DATE)!!

        /* Insert ContentValues into database and get a row ID back */
        val firstRowId = database.insert(
                REFLECTED_TABLE_NAME, null,
                testWeatherValues)

        /* Delete the row we just inserted to see if the database will reuse the rowID */
        database.delete(
                REFLECTED_TABLE_NAME,
                "_ID == " + firstRowId, null)

        /*
         * Now we need to change the date associated with our test content values because the
         * database policy is to replace identical dates on conflict.
         */
        val dayAfterOriginalDate = originalDate + TimeUnit.DAYS.toMillis(1)
        testWeatherValues.put(REFLECTED_COLUMN_DATE, dayAfterOriginalDate)

        /* Insert ContentValues into database and get another row ID back */
        val secondRowId = database.insert(
                REFLECTED_TABLE_NAME, null,
                testWeatherValues)

        val sequentialInsertsDoNotAutoIncrementId = "IDs were reused and shouldn't be if autoincrement is setup properly."
        assertNotSame(sequentialInsertsDoNotAutoIncrementId,
                firstRowId, secondRowId)
    }

    /**
     * This method tests that our database contains all of the tables that we think it should
     * contain. Although in our case, we just have one table that we expect should be added
     *
     *
     * [com.example.android.sunshine.data.TABLE_NAME].
     *
     *
     * Despite only needing to check one table name in Sunshine, we set this method up so that
     * you can use it in other apps to test databases with more than one table.
     */
    @Test
    fun testCreateDb() {
        /*
         * Will contain the name of every table in our database. Even though in our case, we only
         * have only table, in many cases, there are multiple tables. Because of that, we are
         * showing you how to test that a database with multiple tables was created properly.
         */
        val tableNameHashSet = HashSet<String>()

        /* Here, we add the name of our only table in this particular database */
        tableNameHashSet.add(REFLECTED_TABLE_NAME)
        /* Students, here is where you would add any other table names if you had them */
        //        tableNameHashSet.add(MyAwesomeSuperCoolTableName);
        //        tableNameHashSet.add(MyOtherCoolTableNameThatContainsOtherCoolData);

        /* We think the database is open, let's verify that here */
        val databaseIsNotOpen = "The database should be open and isn't"
        assertEquals(databaseIsNotOpen,
                true,
                database.isOpen)

        /* This Cursor will contain the names of each table in our database */
        val tableNameCursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'", null)

        /*
         * If tableNameCursor.moveToFirst returns false from this query, it means the database
         * wasn't created properly. In actuality, it means that your database contains no tables.
         */
        val errorInCreatingDatabase = "Error: This means that the database has not been created correctly"
        assertTrue(errorInCreatingDatabase,
                tableNameCursor.moveToFirst())

        /*
         * tableNameCursor contains the name of each table in this database. Here, we loop over
         * each table that was ACTUALLY created in the database and remove it from the
         * tableNameHashSet to keep track of the fact that was added. At the end of this loop, we
         * should have removed every table name that we thought we should have in our database.
         * If the tableNameHashSet isn't empty after this loop, there was a table that wasn't
         * created properly.
         */
        do {
            tableNameHashSet.remove(tableNameCursor.getString(0))
        } while (tableNameCursor.moveToNext())

        /* If this fails, it means that your database doesn't contain the expected table(s) */
        assertTrue("Error: Your database was created without the expected tables.",
                tableNameHashSet.isEmpty())

        /* Always close a cursor when you are done with it */
        tableNameCursor.close()
    }

    /**
     * This method tests inserting a single record into an empty table from a brand new database.
     * It will fail for the following reasons:
     *
     *
     * 1) Problem creating the database
     * 2) A value of -1 for the ID of a single, inserted record
     * 3) An empty cursor returned from query on the weather table
     * 4) Actual values of weather data not matching the values from TestUtilities
     */
    @Test
    fun testInsertSingleRecordIntoWeatherTable() {

        /* Obtain weather values from TestUtilities */
        val testWeatherValues = TestUtilities.createTestWeatherContentValues()

        /* Insert ContentValues into database and get a row ID back */
        val weatherRowId = database.insert(
                REFLECTED_TABLE_NAME, null,
                testWeatherValues)

        /* If the insert fails, database.insert returns -1 */
        val insertFailed = "Unable to insert into the database"
        assertTrue(insertFailed, weatherRowId != -1L)

        /*
         * Query the database and receive a Cursor. A Cursor is the primary way to interact with
         * a database in Android.
         */
        val weatherCursor = database.query(
                /* Name of table on which to perform the query */
                REFLECTED_TABLE_NAME, null, null, null, null, null, null)/* Columns; leaving this null returns every column in the table *//* Optional specification for columns in the "where" clause above *//* Values for "where" clause *//* Columns to group by *//* Columns to filter by row groups */

        /* Cursor.moveToFirst will return false if there are no records returned from your query */
        val emptyQueryError = "Error: No Records returned from weather query"
        assertTrue(emptyQueryError,
                weatherCursor.moveToFirst())

        /* Verify that the returned results match the expected results */
        val expectedWeatherDidntMatchActual = "Expected weather values didn't match actual values."
        TestUtilities.validateCurrentRecord(expectedWeatherDidntMatchActual,
                weatherCursor,
                testWeatherValues)

        /*
         * Since before every method annotated with the @Test annotation, the database is
         * deleted, we can assume in this method that there should only be one record in our
         * Weather table because we inserted it. If there is more than one record, an issue has
         * occurred.
         */
        assertFalse("Error: More than one record returned from weather query",
                weatherCursor.moveToNext())

        /* Close cursor */
        weatherCursor.close()
    }

    companion object {

        /*
     * In order to verify that you have set up your classes properly and followed our TODOs, we
     * need to create what's called a Change Detector Test. In almost any other situation, these
     * tests are discouraged, as they provide no real value in a production setting. However, using
     * reflection to verify that you have set your classes up correctly will help provide more
     * useful errors if you've missed a step in our instructions.
     *
     * Additionally, using reflection for these tests allows you to run the tests when they
     * normally wouldn't compile, as they depend on pieces of your classes that you might not
     * have created when you initially run the tests.
     */
        private val packageName = "com.example.android.sunshine"
        private val dataPackageName = packageName + ".data"
        private val weatherContractName = ".WeatherContract"
        private val weatherEntryName = weatherContractName + "\$WeatherEntry"
        private val weatherDbHelperName = ".WeatherDbHelper"

        private val databaseNameVariableName = "DATABASE_NAME"
        private var REFLECTED_DATABASE_NAME: String? = null

        private val databaseVersionVariableName = "DATABASE_VERSION"
        private var REFLECTED_DATABASE_VERSION: Int = 0

        private val tableNameVariableName = "TABLE_NAME"
        lateinit var REFLECTED_TABLE_NAME: String

        private val columnDateVariableName = "COLUMN_DATE"
        lateinit var REFLECTED_COLUMN_DATE: String

        private val columnWeatherIdVariableName = "COLUMN_WEATHER_ID"
        lateinit var REFLECTED_COLUMN_WEATHER_ID: String

        private val columnMinVariableName = "COLUMN_MIN_TEMP"
        lateinit var REFLECTED_COLUMN_MIN: String

        private val columnMaxVariableName = "COLUMN_MAX_TEMP"
        lateinit var REFLECTED_COLUMN_MAX: String

        private val columnHumidityVariableName = "COLUMN_HUMIDITY"
        lateinit var REFLECTED_COLUMN_HUMIDITY: String

        private val columnPressureVariableName = "COLUMN_PRESSURE"
        lateinit var REFLECTED_COLUMN_PRESSURE: String

        private val columnWindSpeedVariableName = "COLUMN_WIND_SPEED"
        lateinit var REFLECTED_COLUMN_WIND_SPEED: String

        private val columnWindDirVariableName = "COLUMN_DEGREES"
        lateinit var REFLECTED_COLUMN_WIND_DIR: String
    }
}