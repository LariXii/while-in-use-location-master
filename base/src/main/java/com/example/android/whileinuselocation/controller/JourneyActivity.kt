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
package com.example.android.whileinuselocation.controller

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Chronometer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.android.whileinuselocation.BuildConfig
import com.example.android.whileinuselocation.R
import com.example.android.whileinuselocation.SharedPreferenceUtil
import com.example.android.whileinuselocation.model.Journey
import com.example.android.whileinuselocation.model.ServiceInformations
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_journey.*
import java.security.Permission
import java.time.format.DateTimeFormatter
import java.util.*


private const val TAG = "TruckTracker_Journey"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

/**
 *  This app allows a user to receive location updates without the background permission even when
 *  the app isn't in focus. This is the preferred approach for Android.
 *
 *  It does this by creating a foreground service (tied to a Notification) when the
 *  user navigates away from the app. Because of this, it only needs foreground or "while in use"
 *  location permissions. That is, there is no need to ask for location in the background (which
 *  requires additional permissions in the manifest).
 *
 *  Note: Users have the following options in Android 11+ regarding location:
 *
 *  * Allow all the time
 *  * Allow while app is in use, i.e., while app is in foreground (new in Android 10)
 *  * Allow one time use (new in Android 11)
 *  * Not allow location at all
 *
 * It is generally recommended you only request "while in use" location permissions (location only
 * needed in the foreground), e.g., fine and coarse. If your app has an approved use case for
 * using location in the background, request that permission in context and separately from
 * fine/coarse location requests. In addition, if the user denies the request or only allows
 * "while-in-use", handle it gracefully. To see an example of background location, please review
 * {@link https://github.com/android/location-samples/tree/master/LocationUpdatesBackgroundKotlin}.
 *
 * Android 10 and higher also now requires developers to specify foreground service type in the
 * manifest (in this case, "location").
 *
 * For the feature that requires location in the foreground, this sample uses a long-running bound
 * and started service for location updates. The service is aware of foreground status of this
 * activity, which is the only bound client in this sample.
 *
 * While getting location in the foreground, if the activity ceases to be in the foreground (user
 * navigates away from the app), the service promotes itself to a foreground service and continues
 * receiving location updates.
 *
 * When the activity comes back to the foreground, the foreground service stops, and the
 * notification associated with that foreground service is removed.
 *
 * While the foreground service notification is displayed, the user has the option to launch the
 * activity from the notification. The user can also remove location updates directly from the
 * notification. This dismisses the notification and stops the service.
 */
class JourneyActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object{
        private const val REQUEST_CHECK_SETTINGS = 1
    }

    private var journeyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var journeyLocationService: JourneyLocationService? = null

    // Listens for location broadcasts from JourneyLocationService.
    private lateinit var journeyLocationServiceBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    //Objet permettant de sauvegarder des informations sur l'application une fois quittée
    private lateinit var sharedPreferences:SharedPreferences

    // Monitors connection to the while-in-use service.
    private val journeyLocationServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as JourneyLocationService.LocalBinder
            journeyLocationService = binder.service
            journeyLocationServiceBound = true
            //Log.d(TAG, "Le service tourne : ${journeyLocationService!!.serviceRunning}")
            //Lors du bind au service change les préférences si celui-ci n'est pas en train de tourner (arrive lors de la relance de l'application via Android Studio)
            SharedPreferenceUtil.saveLocationTrackingPref(applicationContext,journeyLocationService!!.serviceRunning)

            updateButtonState(
                sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            )
            //Perform a locationSettingsRequest to get the locationSettingsStates
            requestLocationSettingsEnable()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            journeyLocationService = null
            journeyLocationServiceBound = false
        }
    }

    //Au lancement de l'application
    override fun onCreate(savedInstanceState: Bundle?) {
        //Log.d(TAG,"onCreate Activity")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey)

        journeyLocationServiceBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        // Listener onClick sur le bouton de lancement/arrêt des updates de localisations
        foreground_only_location_button.setOnClickListener {
            //Récupèration de l'état de l'application
            val enabled = sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)

            //Si la récupération des localisations était en cours on l'arrête
            if (enabled) {
                //Lors d'un prochain clic on stop l'update de la localisation
                journeyLocationService?.unsubscribeToLocationUpdates()

                val intentEnd = Intent(this,EndActivity::class.java)
                intentEnd.putExtra("journey", journeyLocationService?.getJourney())
                intentEnd.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intentEnd)
                finish()
            } else {
                // TODO: Step 1.0, Review Permissions: Checks and requests if needed.
                // Si la permission de localisation est approuvé, on lance la récupération des localisations
                if (locationPermissionApproved()) {
                    journeyLocationService?.subscribeToLocationUpdates()
                        ?: Log.d(TAG, "Service Not Bound")
                }
                // Sinon on envoi la demande de permission
                else {
                    requestForegroundPermissions()
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        //Log.d(TAG,"onStart Activity")

        //Lors d'un changement d'une préférence appelle un listener qui est this
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            journeyLocationServiceBroadcastReceiver,IntentFilter(JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            journeyLocationServiceBroadcastReceiver,IntentFilter(JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            journeyLocationServiceBroadcastReceiver,IntentFilter(JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY)
        )

        //Liaison du service de localisation avec l'activité principale
        val serviceIntent = Intent(this, JourneyLocationService::class.java)
        bindService(serviceIntent, journeyLocationServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG,"onResume Activity")
    }

    override fun onPause() {
        //Log.d(TAG,"onPause Activity")
        /*LocalBroadcastManager.getInstance(this).unregisterReceiver(
            journeyLocationServiceBroadcastReceiver
        )*/
        super.onPause()
    }

    override fun onStop() {
        //Log.d(TAG,"onStop Activity")
        if (journeyLocationServiceBound) {
            unbindService(journeyLocationServiceConnection)
            journeyLocationServiceBound = false
        }
        //Enlève le listener associé aux changements de préférences
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        //Enlève le listener sur le LocalBroadCast
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            journeyLocationServiceBroadcastReceiver
        )
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy Activity")
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Lors d'un changement d'une préférence, si la clé modifiée est KEY_FOREGROUND_ENABLED
        // alors on change l'état du bouton de l'interface
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            updateButtonState(sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            )
        }
    }

    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun locationPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    private fun requestForegroundPermissions() {
        val provideRationale = locationPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@JourneyActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@JourneyActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    // TODO: Step 1.0, Review Permissions: Handles permission result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")

                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    journeyLocationService?.subscribeToLocationUpdates()

                else -> {
                    // Permission denied.
                    updateButtonState(false)
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    private fun requestLocationSettingsEnable(){
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        Log.d(TAG,"onRequestLocationSettings()")
        val builderSettingsLocation: LocationSettingsRequest.Builder  = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val task = LocationServices.getSettingsClient(this).checkLocationSettings(builderSettingsLocation.build())

        task.addOnSuccessListener { response ->
            val states = response.locationSettingsStates
            journeyLocationService?.setServiceInformationsStates(states.isGpsPresent,states.isGpsUsable)
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    Log.d(TAG,"La localisation n'est pas activé envoi d'une résolution")
                    // Handle result in onActivityResult()
                    e.startResolutionForResult(this,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) { }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val states: LocationSettingsStates = LocationSettingsStates.fromIntent(data)
        when(requestCode) {
            REQUEST_CHECK_SETTINGS ->
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // All required changes were successfully made
                    Log.d(TAG,"Service bound : ${journeyLocationService?: "null"}")
                    journeyLocationService?.setServiceInformationsStates(states.isGpsPresent,states.isGpsUsable)
                    Log.d(TAG,"GPS Present : ${states.isGpsPresent}\nGPS Usable : ${states.isGpsUsable}")
                }
                Activity.RESULT_CANCELED -> {
                    journeyLocationService?.setServiceInformationsStates(states.isGpsPresent,states.isGpsUsable)
                    Log.d(TAG,"GPS Present : ${states.isGpsPresent}\nGPS Usable : ${states.isGpsUsable}")
                    Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
                else -> {

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    /**
     * Fonction pour changer le texte du bouton de l'interface
     */
    private fun updateButtonState(trackingLocation: Boolean) {
        foreground_only_location_button.isChecked = trackingLocation
        if(trackingLocation){
            (chronometer_text as Chronometer).base = journeyLocationService!!.getJourney().startTime
            (chronometer_text as Chronometer).start()
            journeyInformationsToScreen(journeyLocationService!!.getJourney())
        }
        else{
            (chronometer_text as Chronometer).base = SystemClock.elapsedRealtime()
            (chronometer_text as Chronometer).stop()
        }
    }

    /**
     * Fonction pour afficher les résultats de localisations.
     * Récupère le texte déjà présent dans la zone de texte et rajoute le nouveau texte
     */
    private fun serviceInformationsToScreen(serviceInfos: ServiceInformations) {
        textView_error_msg.isVisible = !serviceInfos.isGpsUsable

        if(serviceInfos.isGpsUsable){
            gps_unable.text = "Activé"
            gps_unable.setTextColor(Color.GREEN)
        }
        else{
            gps_unable.text = "Désactivé"
            gps_unable.setTextColor(Color.RED )
        }

        send_file_text.isVisible = serviceInfos.isSending
        Log.d(TAG,"Envoi du fichier il doit etre visible ? : ${serviceInfos.isSending}")
    }

    private fun journeyInformationsToScreen(journey: Journey) {
        container_journey_infos.isVisible = journey.isPending()
        number_loc_text.text = journey.numberOfLocalisation().toString()
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        textView_date_start_journey.text = journey.getStartDateTime().format(dateFormatter)
    }

    /**
     * Receiver for location broadcasts from [JourneyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                // LOCATION
                JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS")
                    val serviceInfos = intent.getParcelableExtra<ServiceInformations>(
                        JourneyLocationService.EXTRA_INFORMATIONS
                    )

                    if (serviceInfos != null) {
                        serviceInformationsToScreen(serviceInfos)
                    }
                }
                // JOURNEY
                JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS")
                    val journeyInfo = intent.getParcelableExtra<Journey>(
                        JourneyLocationService.EXTRA_JOURNEY
                    )

                    if (journeyInfo != null) {
                        // TODO Display journey informations
                        journeyInformationsToScreen(journeyInfo)
                    }
                }
                // CHECK_REQUEST
                JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST")
                    val pendingIntent = intent.getParcelableExtra<PendingIntent>(
                        JourneyLocationService.EXTRA_CHECK_REQUEST
                    )

                    if(pendingIntent != null) {
                        try {
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                REQUEST_CHECK_SETTINGS,
                                null,
                                0,
                                0,
                                0
                            )
                        } catch (e: SendIntentException) {
                            // Ignore the error
                        }
                    }
                }
                else -> {

                }
            }
        }
    }
}
