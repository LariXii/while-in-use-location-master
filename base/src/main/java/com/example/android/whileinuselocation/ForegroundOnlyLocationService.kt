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

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */
class ForegroundOnlyLocationService : Service() {
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private var idLocation = 0

    private var fileStream: FileOutputStream? = null

    private lateinit var fileWritting: String

    private val filesUploading: MutableList<String> = mutableListOf()

    private lateinit var user: User

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

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

            timerHandler.postDelayed(this, (TIME_TO_WAIT_BEFORE_SEND_MAPM * 1000 * 60).toLong())
        }
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

                Log.d(TAG,"Nombre de localisations disponible : ${locationResult?.locations}")

                if (locationResult?.locations != null) {

                    for(loc in locationResult.locations){
                        currentLocation = Localisation(idLocation++, loc)
                        user.addLocation(currentLocation!!)

                        val intent = Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
                        intent.putExtra(EXTRA_LOCATION, currentLocation)
                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

                        // Ecriture des localisations dans le fichier interne ouvert
                        fileStream?.write(currentLocation.toString().toByteArray())
                    }
                    // Normally, you want to save a new location to a database. We are simplifying
                    // things a bit and just saving it as a local variable, as we only need it again
                    // if a Notification is created (when user navigates away from app).

                    //currentLocation = Localisation(idLocation++, locationResult.lastLocation)
                    //user.addLocation(currentLocation!!)

                    Log.d(TAG, "Nombre de localisation récupérés : ${user.numberOfLocalisation()}")
                    // Notify our Activity that a new location was added. Again, if this was a
                    // production app, the Activity would be listening for changes to a database
                    // with new locations, but we are simplifying things a bit to focus on just
                    // learning the location side of things.

                    //val intent = Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
                    //intent.putExtra(EXTRA_LOCATION, currentLocation)
                    //LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

                    // Updates notification content if this service is running as a foreground
                    // service.
                    /*if (serviceRunningInForeground) {
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            generateNotification(currentLocation))
                    }*/
                } else {
                    Log.d(TAG, "Location information isn't available.")
                }
            }
        }

        // TODO Supprimer tous les fichiers (clean up)
        /*
        cleanUpFiles()

        Log.d(TAG,"Liste des fichiers sauvegardés : \n")
        for(f in files){
            Log.d(TAG,"\t-$f size : ${File(applicationContext.filesDir, f).length()}octets\n")
        }*/

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        //Valeur de la clé extra CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION
        val cancelLocationTrackingFromNotification =
            intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        //Si cette valeur est égal à true
        if (cancelLocationTrackingFromNotification) {
            //Arrêt des requêtes de localisation
            unsubscribeToLocationUpdates()
            //Arrêt du service
            stopSelf()
        }
        // Tells the system not to recreate the service after it's been killed.
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
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
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
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

        try {
            // TODO: Step 1.5, Subscribe to location changes.
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

            user = User("Test",1,4,2)
            timerHandler.postDelayed(timerRunnable, (TIME_TO_WAIT_BEFORE_SEND_MAPM * 1000 * 60).toLong());

            createFileToWrite()

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
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
                    stopSelf()
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }

            // Fermeture du fichier interne
            fileStream?.close()
            // Envoi du fichier en cours d'écriture à la fin du service
            uploadFile()

            timerHandler.removeCallbacks(timerRunnable);

            SharedPreferenceUtil.saveLocationTrackingPref(this, false)

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    private fun createFileToWrite(){
        // Création du fichier
        val name = nameMAPMFiles()
        File(applicationContext.filesDir, name)
        // Sauvegarde du nom du fichier en cours d'écriture
        fileWritting = name
        //Ajout du fichier dans la liste des fichiers à uploader
        filesUploading.add(name)
        // Ouverture du fichier
        fileStream = applicationContext.openFileOutput(fileWritting, Context.MODE_PRIVATE)
        Log.d(TAG,"Fichier en cours d'écriture : $fileWritting")
    }

    private fun cleanUpFiles(){
        val files = applicationContext.fileList()
        Log.d(TAG,"Suppression de tous les fichiers\n")
        for(f in files){
            File(applicationContext.filesDir, f).delete()
        }
    }

    private fun nameMAPMFiles(): String{
        //Récupération de la date et du temps à l'appel de cette fonction
        val date: LocalDateTime = LocalDateTime.now()
        //Initialisation des formatters
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val timeFormatter = DateTimeFormatter.ofPattern("HHmmss")
        //Initialisation de la date et du temps sous le bon format à l'aide de leur formatter respectif
        val parsedDate: String = date.format(dateFormatter)
        val parsedTime: String = date.format(timeFormatter)
        //Création du nom du fichier
        return "${parsedDate}_${parsedTime}_${TYPE_FILE}_${CREATOR_ID}_${CHECK_SUM_ID}.$EXTENSION"
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
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        notificationCompatBuilder
            .setContentTitle("Envoi du fichier")
            .setContentText("Envoi en cours")
            .setSmallIcon(R.drawable.ic_upload)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return notificationCompatBuilder
    }

    private fun uploadFile(){
        //Création de la notification pour afficher l'upload du fichier
        val notificationCompatBuilder = createNotificationUpload()

        // Start a the operation in a background thread
        Thread(
            Runnable {
                Log.d(TAG, "Envoi du fichier : ${filesUploading[0]}")
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
                    incr += 5
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
                File(applicationContext.filesDir, s).delete()

                //Affichage des fichiers dans la mémoire interne
                val files = applicationContext.fileList()
                Log.d(TAG,"Liste des fichiers dans la mémoire interne après envoi : \n")
                for(f in files){
                    Log.d(TAG,"\t-$f size : ${File(applicationContext.filesDir, f).length()}octets\n")
                }

            } // Starts the thread by calling the run() method in its Runnable
        ).start()
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

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0)

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            //.setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_launch, getString(R.string.launch_activity),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_cancel,
                getString(R.string.stop_location_updates_button_text),
                servicePendingIntent
            )
            .build()
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private const val TAG = "ForegroundOnlyLocationService"

        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_FILE_ID = 87654321

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"

        private const val NOTIFICATION_CHANNEL_FILE_ID = "while_in_use_channel_02"

        private const val FILENAME = "MAPM_Locations"

        private const val EXTENSION = "csv"

        private const val TYPE_FILE = "MAPM"

        private const val CREATOR_ID = "ea0bb5a01de9"

        private const val CHECK_SUM_ID = "4680ee8bd1607273607ae2de20fb2e10"

        private const val TIME_TO_WAIT_BEFORE_SEND_MAPM = 1
    }
}

