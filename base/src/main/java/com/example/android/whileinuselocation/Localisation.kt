package com.example.android.whileinuselocation

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.*

class Localisation(private val id: Int, val location: Location?): Parcelable{

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readParcelable(Location::class.java.classLoader)
    ) {
    }

    override fun toString(): String {
        val speed = location?.speed
        val speedAccuracyMetersPerSecond = location?.speedAccuracyMetersPerSecond

        val bearing = location?.bearing
        val bearingAccuracyDegrees = location?.bearingAccuracyDegrees

        val toSecond = 1000000000
        val time = location?.elapsedRealtimeNanos?.div(toSecond)
        val sTime = SystemClock.elapsedRealtimeNanos()/toSecond

        //val date = Date.getTime()
        //val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        //val TimeStamp = date.format(formatter)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDateandTime: String = sdf.format(Date())

        if (location != null) {
            return "[IDENTIFIANT] : $id\nLat : ${location.latitude}, Long : ${location.longitude}\nPrécision : ${location.accuracy/5}, Cap : $bearing ± $bearingAccuracyDegrees\nDate : $currentDateandTime\nÂge du fix : ${sTime- time!!} s\nVitesse : $speed ± $speedAccuracyMetersPerSecond"
        }
        else{
            return "No location"
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeParcelable(location, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Localisation> {
        override fun createFromParcel(parcel: Parcel): Localisation {
            return Localisation(parcel)
        }

        override fun newArray(size: Int): Array<Localisation?> {
            return arrayOfNulls(size)
        }
    }
}