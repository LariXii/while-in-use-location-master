/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.whileinuselocation

import android.content.Context
import android.location.Location
import android.os.SystemClock
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Returns the `location` object as a human readable string.
 */
/*fun Localisation?.toText():String {

    val speed = this?.location?.speed
    val speedAccuracyMetersPerSecond = this?.speedAccuracyMetersPerSecond

    val bearing = this?.bearing
    val bearingAccuracyDegrees = this?.bearingAccuracyDegrees

    val toSecond = 1000000000
    val time = this?.elapsedRealtimeNanos?.div(toSecond)
    val sTime = SystemClock.elapsedRealtimeNanos()/toSecond

    //val date = Date.getTime()
    //val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    //val TimeStamp = date.format(formatter)

    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss z")
    val currentDateandTime: String = sdf.format(Date())

    return if (this != null) {
        "Lat : $latitude, Long : $longitude\nPrécision : ${accuracy/5}, Cap : $bearing ± $bearingAccuracyDegrees\nDate : $currentDateandTime\nÂge du fix : ${sTime- time!!} s\nVitesse : $speed ± $speedAccuracyMetersPerSecond\n\n${this}"
    } else {
        "Unknown location"
    }
}*/

/**
 * Provides access to SharedPreferences for location to Activities and Services.
 */
internal object SharedPreferenceUtil {

    //Clé pour stocker l'état du service
    const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"
    //Clé pour savoir si l'application s'est terminé avec un crash
    const val KEY_APPLICATION_CRASHED = "application_crashed"
    //Clé du nom de l'utilisateur
    const val KEY_USER_NAME = "user_name"
    //Clé du type de roue
    const val KEY_TYRE_TYPE = "tyre_type"
    //Clé de l'essieu du tracteur
    const val KEY_TRACTOR_AXLES = "tractor_axles"
    //Clé de l'essieu de la remorque
    const val KEY_TRAILER_AXLES = "trailer_axles"

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The [Context].
     */
    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE).getBoolean(KEY_FOREGROUND_ENABLED, false)

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE).edit {
            putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
        }

    fun saveMenuPref(context: Context, key: String, value: String){
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE).edit {
            putString(key, value)
        }
    }

    fun getMenuPref(context: Context, key: String): Boolean = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE).getBoolean(key, false)
}
