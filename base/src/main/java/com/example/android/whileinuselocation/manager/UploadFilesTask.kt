package com.example.android.whileinuselocation.manager

import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import com.example.android.whileinuselocation.controller.JourneyLocationService
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.lang.IllegalArgumentException
import kotlin.system.exitProcess

class UploadFilesTask(private val fileHandler: Handler): AsyncTask<String, Integer, Boolean>() {
    private val TAG = "TruckTracker_UploadFilesTask"
    override fun doInBackground(vararg params: String?): Boolean {
        //Todo
        if(params.size != 4){
            throw IllegalArgumentException("Error, might have 4 argument")
        }
        val serverName = params[0]!!
        val ftpClient = FTPClient()

        val login = params[1]!!
        val password = params[2]!!
        val fileName = params[3]!!

        ftpClient.connect(serverName)
        Log.d(TAG,"Connection au serveur FTP")
        ftpClient.login(login,password)
        Log.d(TAG,"Login au serveur FTP")

        Log.d(TAG,"Reply message : ${ftpClient.replyString}Reply code : ${ftpClient.replyCode}")
        val reply = ftpClient.replyCode

        if(!FTPReply.isPositiveCompletion(reply)){
            ftpClient.disconnect()
            Log.e(TAG,"FTP server refused connection")
            return false
        }

        //Transfer File
        ftpClient.storeFile("mapm_files/$fileName",FileInputStream(File(fileName)))
        Log.d(TAG,"StoreFile")
        //Start notification
        //fileHandler.obtainMessage(JourneyLocationService.STATE_SEND).sendToTarget()

        ftpClient.logout()
        ftpClient.disconnect()

        return true
    }

    /*override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if(result!!){
            //Notification stop
            fileHandler.obtainMessage(JourneyLocationService.STATE_SENT).sendToTarget()
            Log.d(TAG,"Fichier envoyé")
        }
        else{
            //Notification print error
            fileHandler.obtainMessage(JourneyLocationService.STATE_ERROR).sendToTarget()
            Log.d(TAG,"Error lors de l'envoi")
        }
        try {
            // Sleep for 1 second
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            Log.d("TAG", "sleep failure")
        }
        fileHandler.obtainMessage(JourneyLocationService.STATE_CLOSE).sendToTarget()
        Log.d(TAG,"Fin")
    }*/
}
