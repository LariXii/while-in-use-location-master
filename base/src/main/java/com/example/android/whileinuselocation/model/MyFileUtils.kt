package com.example.android.whileinuselocation.model

import android.content.Context
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyFileUtils {
    companion object{
        private const val TAG = "TruckTracker_FILE"

        const val TYPE_MAPM = "MAPM"
        const val TYPE_EVNT = "EVNT"
        const val CREATOR_ID = "ea0bb5a01de9"
        const val CHECK_SUM_ID = "4680ee8bd1607273607ae2de20fb2e10"
        const val CSV_EXT = "csv"

        fun nameFiles(type: String, ext: String): String{
            //Récupération de la date et du temps à l'appel de cette fonction
            val date: LocalDateTime = LocalDateTime.now()
            //Initialisation des formatters
            val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val timeFormatter = DateTimeFormatter.ofPattern("HHmmss")
            //Initialisation de la date et du temps sous le bon format à l'aide de leur formatter respectif
            val parsedDate: String = date.format(dateFormatter)
            val parsedTime: String = date.format(timeFormatter)
            //Création du nom du fichier
            return "${parsedDate}_${parsedTime}_${type}_${CREATOR_ID}_${CHECK_SUM_ID}.${ext}"
        }

        fun cleanUpFiles(context: Context){
            val files = context.fileList()
            Log.d(TAG,"Suppression de tous les fichiers\n")
            for(f in files){
                File(context.filesDir, f).delete()
            }
        }
    }
}