package com.example.android.whileinuselocation.manager

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.example.android.whileinuselocation.model.Event
import com.example.android.whileinuselocation.model.MyFileUtils
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This classe is used to create event file and manage his writting content
 */
class EventManager(_context: Context, _obuId: Int, _manufacturerId: Int, _contractProviderId: Int) {
    private var fileStream: FileOutputStream? = null
    private val emApplicationContext = _context
    private var name: String? = null

    //ID row
    private var c1: Int = 0
    //OBU ID
    private val c2: Int = _obuId
    //MANUFACTURER ID
    private val c3: Int = _manufacturerId
    //CONTRACT PROVIDER ID
    private val c4: Int = _contractProviderId

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    fun openFile(){
        name = MyFileUtils.nameFiles(MyFileUtils.TYPE_EVNT, MyFileUtils.CSV_EXT)
        File(emApplicationContext.filesDir, name!!)
        fileStream = emApplicationContext.openFileOutput(name, Context.MODE_PRIVATE)
    }
    fun closeFile(): String{
        assert(fileStream != null)
        assert(name != null)

        fileStream?.close()
        return name!!
    }
    fun writeEvent(evnt: Event, et: Long){
        assert(fileStream != null)

        val toSecond = 1000000000
        //Calcul de la date précise à laquelle est arrivé l'event
        val etCurrent = SystemClock.elapsedRealtimeNanos()
        val diff = (etCurrent - et) / toSecond
        //Création de l'objet date
        val date = LocalDateTime.now().minusSeconds(diff)
        val c10 = date.format(formatter)
        //ProxyTime
        val c11 = null

        fileStream?.write("$c1;$c2;$c3;$c4;$evnt;\"$c10\";${c11?:""};\r\n".toByteArray())
        c1++
    }
}