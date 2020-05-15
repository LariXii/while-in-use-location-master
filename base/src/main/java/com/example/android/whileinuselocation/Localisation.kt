package com.example.android.whileinuselocation

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Localisation(private val _sequenceNumber: Int, private val location: Location, private val _tyreType: Int, private val _tractorAxles: Int, private val _trailerAxles: Int): Parcelable{

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readParcelable(Location::class.java.classLoader)
    ) {
    }
    private val c1: Int = 399367311
    private val c2: Int = 3
    private val c3: Int = 10747906
    private val c4: Int = _tyreType
    private val c5: Int = _trailerAxles
    private val c6: Int = _tractorAxles
    private val c7: String
    private val c8: Double = location.latitude
    private val c9: Double = location.longitude
    private val c10: Float = location.speed
    private val c11: Float = location.bearing
    private val c12: Float = location.accuracy/5
    private val c13: Int = 20
    private val c14: Int = _sequenceNumber
    private val c15: Double = location.altitude
    private val c16: String = ""
    private val c17: String = "DE"
    private val c18: Int = 16383

    init{
        val date = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        c7 = date.format(formatter)
    }

    override fun toString(): String {
        val speed = location.speed
        val speedAccuracyMetersPerSecond = location.speedAccuracyMetersPerSecond

        val bearing = location.bearing
        val bearingAccuracyDegrees = location.bearingAccuracyDegrees

        val toSecond = 1000000000
        val time = location.elapsedRealtimeNanos?.div(toSecond)
        val sTime = SystemClock.elapsedRealtimeNanos()/toSecond

        val date = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val parsedDate = date.format(formatter)

        //val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        //val currentDateandTime: String = sdf.format(Date())

        if (location != null) {
            return "$c14;${location.latitude};${location.longitude};${location.accuracy/5};$bearing ± $bearingAccuracyDegrees;$parsedDate;${sTime- time!!};$speed ± $speedAccuracyMetersPerSecond\n"
        }
        else{
            return "No location"
        }
    }

    fun toMAPM(){

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