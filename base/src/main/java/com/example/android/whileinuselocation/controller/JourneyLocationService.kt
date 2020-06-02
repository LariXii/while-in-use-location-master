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

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.android.whileinuselocation.R
import com.example.android.whileinuselocation.SharedPreferenceUtil
import com.example.android.whileinuselocation.manager.EventManager
import com.example.android.whileinuselocation.model.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.io.File
import java.io.FileOutputStream

/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */
class JourneyLocationService : Service() {
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    var serviceRunning = false

    private var idLocation = 0

    private var noFixHappened = 0

    // ########################### FILES ########################### //
    private var fileStream: FileOutputStream? = null

    private lateinit var fileWriting: String

    private val filesUploading: MutableList<String> = mutableListOf()
    // ############################################################# //

    private lateinit var serviceInformations: ServiceInformations

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    private lateinit var locationManager: LocationManager

    private lateinit var eventManager: EventManager

    private val contextServiceBroadcastReceiver = ContextServiceBroadcastReceiver()

    // TODO: Step 1.1, Review variables (no changes).
    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    internal lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Localisation? = null

    private var journey: Journey? = null

    val countDownTimerNoFix = CountDownTimerNoFix(TIME_TO_WAIT_FOR_NO_FIX,1000)
    var timerHandler: Handler = Handler()
    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.d(TAG,"Fichier à uploader : $filesUploading")

            var files = applicationContext.fileList()
            Log.d(TAG,"Liste des fichiers dans la mémoire interne avant envoi : \n")
            for(f in files){
                Log.d(TAG,"\t-$f size : ${File(applicationContext.filesDir, f).length()}octets\n")
            }

            //Création du nouveau fichier dans lequel écrire et changement du flux sur celui-ci
            createFileToWrite()

            //Envoi du fichier au serveur
            uploadFile()

            timerHandler.postDelayed(this, (TIME_TO_WAIT_BEFORE_SEND_MAPM).toLong())
        }
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Log.d(TAG,"La permission d'utiliser le WIFI pour la localisation est activé : ${locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)}")
        Log.d(TAG,"La permission d'utiliser le GPS est activé : ${locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)}")

        eventManager = EventManager(applicationContext,399367311, 3, 10747906)

        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_LOW))
        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_OKAY))
        //registerReceiver(contextServiceBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        Log.d(TAG,"Pourcentage de batterie :  ${getBatteryPercentage(applicationContext)}")

        serviceInformations = ServiceInformations()

        // TODO: Step 1.2, Review the FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // TODO: Step 1.3, Create a LocationRequest.
        locationRequest = LocationRequest().apply {
            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.
            //
            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            interval = 5 * 1000

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            fastestInterval = 3 * 1000

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = 10 * 1000

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // TODO: Step 1.4, Initialize the LocationCallback.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                Log.d(TAG,"Localisations disponible : ${locationResult?.locations}")
                Log.d(TAG,"Nombre : ${locationResult?.locations?.size}")

                if (locationResult?.locations != null) {

                    countDownTimerNoFix.cancel()
                    countDownTimerNoFix.start()
                    noFixHappened = 0

                    for(loc in locationResult.locations){
                        currentLocation =
                            Localisation(
                                idLocation++,
                                loc,
                                1,
                                2,
                                3
                            )

                        journey!!.addLocation(currentLocation!!)

                        if(loc.isFromMockProvider){
                            eventManager.writeEvent(Event(Event.EVNT_GPS_FROM_MOCK_PROVIDER),SystemClock.elapsedRealtimeNanos())
                        }
                        broadCastServiceInformations()
                        broadCastJourneyInformations()

                        // Ecriture des localisations dans le fichier interne ouvert
                        fileStream?.write(currentLocation!!.toMAPM().toByteArray())
                    }

                } else {
                    Log.d(TAG, "Location information isn't available.")
                }
            }
            override fun onLocationAvailability(availability: LocationAvailability?) {
                super.onLocationAvailability(availability)
                //Log.d(TAG,"onLocationAvailability()")
                if (availability != null) {
                    //Log.d(TAG,"availability != null")
                    if(!availability.isLocationAvailable) {
                        Log.d(TAG,"Location Availability changed ! ")
                        val builderSettingsLocation: LocationSettingsRequest.Builder  = LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest)

                        val task = LocationServices.getSettingsClient(applicationContext).checkLocationSettings(builderSettingsLocation.build())

                        task.addOnSuccessListener { response ->
                            //Log.d(TAG,"task.onSuccess")
                            val states = response.locationSettingsStates
                            // TODO STATES
                            broadCastServiceInformations()
                        }
                        task.addOnFailureListener { e ->
                            //Log.d(TAG,"task.onFailure")
                            if (e is ResolvableApiException) {
                                try {
                                    //Si le service tourne en premier plan
                                    if(serviceRunningInForeground){
                                        //On lance l'activité pour résoudre le problème
                                        val intent = Intent(applicationContext, JourneyActivity::class.java)
                                        startActivity(intent)
                                    }
                                    else{
                                        //On envoi un message à l'activité pour résoudre le problème
                                        val intent = Intent(
                                            ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST
                                        )
                                        intent.putExtra(EXTRA_CHECK_REQUEST, e.resolution)
                                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                                    }
                                    // Handle result in onActivityResult()
                                    //e.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                                } catch (sendEx: IntentSender.SendIntentException) { }
                            }
                        }
                    }
                }
            }
        }

        // TODO Supprimer tous les fichiers (clean up)
        //cleanUpFiles()

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind()")

        // MainActivity (client) comes into foreground and binds to service, so the service can
        // become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(
                this
            )
        ) {
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification(null)
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG,"La configuration a changé ! ")
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        journey = Journey()
        journey!!.startJourney()

        SharedPreferenceUtil.saveLocationTrackingPref(
            this,
            true
        )

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, JourneyLocationService::class.java))
        serviceRunning = true

        try {
            // TODO: Step 1.5, Subscribe to location changes.
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

            //Start the timer to send all 5 min a MAPM file
            timerHandler.postDelayed(timerRunnable, (TIME_TO_WAIT_BEFORE_SEND_MAPM).toLong())
            //Start the timer for evnt no fix and no fix persistent
            countDownTimerNoFix.start()

            //Création du fichier MAPM
            createFileToWrite()
            //Création du fichier de log EVNT
            eventManager.openFile()


        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(
                this,
                false
            )
            countDownTimerNoFix.cancel()
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            // TODO: Step 1.6, Unsubscribe to location changes.
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                    countDownTimerNoFix.cancel()
                    //stopSelf()
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }

            // Arrêt du chronomètre d'envoi des fichiers
            timerHandler.removeCallbacks(timerRunnable)

            // Fermeture des streams des fichiers MAPM et EVNT
            fileStream?.close()

            val nameFileEvnt = eventManager.closeFile()
            //TODO upload nameFileEvnt uploadFile(nameFileEvnt)

            // Envoi du fichier en cours d'écriture à la fin du service
            uploadFile()

            // Arrêt du trajet
            journey!!.stopJourney()

            // Sauvegarde de l'état du service (ici arrêté) dans les préférences
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            serviceRunning = false

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(
                this,
                true
            )
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    fun getJourney(): Journey{
        return journey!!
    }

    fun setServiceInformationsStates(isGpsPresent: Boolean, isGpsUsable: Boolean){
        //Log.d(TAG,"setServiceInformationsStates()")
        serviceInformations.isGpsPresent = isGpsPresent
        serviceInformations.isGpsUsable = isGpsUsable
        broadCastServiceInformations()
    }

    private fun getBatteryPercentage(context: Context): Int{
        val percentage: Int
        if(Build.VERSION.SDK_INT >= 21){
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
        else{
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            val batteryPct: Float? = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            }
            percentage = (batteryPct ?: -1) as Int
        }
        return percentage
    }

    private fun createFileToWrite(){
        // Création du fichier
        val name = MyFileUtils.nameFiles(MyFileUtils.TYPE_MAPM,MyFileUtils.CSV_EXT)
        File(applicationContext.filesDir, name)
        // Sauvegarde du nom du fichier en cours d'écriture
        fileWriting = name
        //Ajout du fichier dans la liste des fichiers à uploader
        filesUploading.add(name)
        // Ouverture du fichier
        fileStream = applicationContext.openFileOutput(fileWriting, Context.MODE_PRIVATE)
        Log.d(TAG,"Fichier en cours d'écriture : $fileWriting")
    }

    private fun createNotificationUpload(): NotificationCompat.Builder{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Création du nouveau Channel de notification
            val name = getString(R.string.notification_file)
            val mChannel = NotificationChannel(NOTIFICATION_CHANNEL_FILE_ID, name, NotificationManager.IMPORTANCE_LOW)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(mChannel)
        }

        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext,
                NOTIFICATION_CHANNEL_ID
            )

        notificationCompatBuilder
            .setContentTitle("Envoi du fichier")
            .setContentText("Envoi en cours")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_upload)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return notificationCompatBuilder
    }

    private fun uploadFile(){
        //Création de la notification pour afficher l'upload du fichier
        val notificationCompatBuilder = createNotificationUpload()

        if(serviceRunning){
            // Start a the operation in a background thread
            Thread(
                Runnable {
                    Log.d(TAG, "Envoi du fichier : ${filesUploading[0]}")
                    serviceInformations.isSending = true
                    broadCastServiceInformations()
                    // Do the "lengthy" operation 20 times
                    var incr: Int = 0
                    while (incr <= 100) {

                        // Sets the progress indicator to a max value, the current completion percentage and "determinate" state
                        notificationCompatBuilder.setProgress(100, incr, false)
                        // Displays the progress bar for the first time.
                        notificationManager.notify(NOTIFICATION_FILE_ID, notificationCompatBuilder.build())
                        // Sleeps the thread, simulating an operation
                        try {
                            // Sleep for 1 second
                            Thread.sleep(1 * 1000.toLong())
                        } catch (e: InterruptedException) {
                            Log.d("TAG", "sleep failure")
                        }
                        incr += 20
                    }
                    // When the loop is finished, updates the notification
                    notificationCompatBuilder.setContentText("Download completed") // Removes the progress bar
                        .setProgress(0, 0, false)
                    notificationManager.notify(NOTIFICATION_FILE_ID, notificationCompatBuilder.build())

                    try {
                        // Sleep for 1 second
                        Thread.sleep(1 * 1000.toLong())
                    } catch (e: InterruptedException) {
                        Log.d("TAG", "sleep failure")
                    }
                    notificationCompatBuilder.setOngoing(false)

                    //Suppression automatique de la notification
                    notificationManager.notify(NOTIFICATION_FILE_ID, notificationCompatBuilder.build())
                    notificationManager.cancel(NOTIFICATION_FILE_ID)

                    //Suppression du fichier une fois uploadé
                    val s = filesUploading.removeAt(0)
                    //File(applicationContext.filesDir, s).delete()

                    //Affichage des fichiers dans la mémoire interne
                    val files = applicationContext.fileList()
                    Log.d(TAG,"Liste des fichiers dans la mémoire interne après envoi : \n")
                    for(f in files){
                        Log.d(TAG,"\t-$f size : ${File(applicationContext.filesDir, f).length()}octets\n")
                    }
                    serviceInformations.isSending = false
                    broadCastServiceInformations()
                } // Starts the thread by calling the run() method in its Runnable
            ).start()
        }
    }

    private fun broadCastServiceInformations(){
        //Log.d(TAG,"broadCastServiceInformations")
        val intent = Intent(ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS)
        intent.putExtra(EXTRA_INFORMATIONS, serviceInformations)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun broadCastJourneyInformations(){
        //Log.d(TAG,"broadCastServiceInformations")
        val intent = Intent(ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY)
        intent.putExtra(EXTRA_JOURNEY, journey)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun generateNotification(text: String?): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data
        val mainNotificationText = text ?: getString(R.string.notification)
        val titleText = getString(R.string.app_name)

        // 1. Create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT)

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, JourneyActivity::class.java)//.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        val launchActivityPendingIntent: PendingIntent = PendingIntent.getActivity(this,0,launchActivityIntent,0)

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext,
                NOTIFICATION_CHANNEL_ID
            )

        return notificationCompatBuilder
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(launchActivityPendingIntent)
            .build()
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: JourneyLocationService
            get() = this@JourneyLocationService
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class CountDownTimerNoFix(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval){
        override fun onFinish() {
            if(noFixHappened >= 5){
                Log.d(TAG,"onFinish CountDownTimerNoFix > 5")
                eventManager.writeEvent(Event(Event.EVNT_GPS_NO_FIX_PERSISTENT), SystemClock.elapsedRealtimeNanos())
            }
            else{
                Log.d(TAG,"onFinish CountDownTimerNoFix")
                eventManager.writeEvent(Event(Event.EVNT_GPS_NO_FIX), SystemClock.elapsedRealtimeNanos())
            }
            noFixHappened++
            countDownTimerNoFix.start()
        }

        override fun onTick(millisUntilFinished: Long) {

        }

    }

    /**
     * Class used to receive some values about services's context (ex : Location service or Battery service).
     */
    inner class ContextServiceBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                //Mode changed
                LocationManager.MODE_CHANGED_ACTION -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_MODE_CHANGED")
                    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    serviceInformations.isGpsUsable = isGpsEnabled
                    broadCastServiceInformations()

                    if(isGpsEnabled || isNetworkEnabled){
                        //Log.d(TAG,"La localisation est activé !")
                    }
                    else{
                        //Log.d(TAG,"La localisation est désactivé !")
                        if(serviceRunning)
                            eventManager.writeEvent(Event(Event.EVNT_GPS_NO_COMMUNICATION), SystemClock.elapsedRealtimeNanos())
                    }
                }
                //Mode changed
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    //Log.d(TAG,"PROVIDERS_CHANGED_ACTION")
                    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if(isGpsEnabled || isNetworkEnabled){
                        //Log.d(TAG,"La localisation est activé !")
                    }
                    else{
                        //Log.d(TAG,"La localisation est désactivé !")
                        if(serviceRunning)
                            eventManager.writeEvent(Event(Event.EVNT_GPS_NO_COMMUNICATION), SystemClock.elapsedRealtimeNanos())
                    }
                }
                Intent.ACTION_BATTERY_LOW -> {
                    //Log.d(TAG,"Le niveau de la batterie est faible : ${getBatteryPercentage(applicationContext)}")
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    //Log.d(TAG,"Le niveau de la batterie est ok : ${getBatteryPercentage(applicationContext)}")
                }
                //Intent.ACTION_BATTERY_CHANGED -> {
                //    Log.d(TAG,"Le niveau de batterie à changé : ${getBatteryPercentage(applicationContext)}")
                //}
                else -> {

                }
            }
        }
    }

    companion object {
        // #################### VARIABLES #################### //
        private const val TAG = "TruckTracker_JourneyService"

        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_FILE_ID = 87654321

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"

        private const val NOTIFICATION_CHANNEL_FILE_ID = "while_in_use_channel_02"

        private const val TIME_TO_WAIT_BEFORE_SEND_MAPM = 20 * 1000

        private const val TIME_TO_WAIT_FOR_NO_FIX: Long = 2 * 60 * 1000

        // ################################################ //

        // #################### ACTIONS #################### //
        internal const val ACTION_SERVICE_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST_LOCATION"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST_JOURNEY"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST =
            "$PACKAGE_NAME.action.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST_CHECK_REQUEST"
        // ################################################ //

        // #################### EXTRAS #################### //
        internal const val EXTRA_INFORMATIONS = "$PACKAGE_NAME.extra.LOCATION"

        internal const val EXTRA_JOURNEY = "$PACKAGE_NAME.extra.JOURNEY"

        internal const val EXTRA_CHECK_REQUEST = "$PACKAGE_NAME.extra.CHECK_REQUEST"
        // ################################################ //
    }
}

