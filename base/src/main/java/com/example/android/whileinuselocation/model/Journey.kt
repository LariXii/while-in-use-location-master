package com.example.android.whileinuselocation.model

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class Journey() : Parcelable {
    private var startDate: LocalDate? = null
    private var startTime: Long = 0

    private var endDate: LocalDate? = null
    private var endTime: Long = 0

    private var duration: Long? = null

    private var isPending = false

    private val listLocalisation: MutableList<Localisation> = mutableListOf()

    constructor(parcel: Parcel) : this() {
        isPending = parcel.readByte() != 0.toByte()
    }

    fun startJourney(){
        startDate = LocalDate.now()
        startTime = SystemClock.elapsedRealtime()
        isPending = true
    }

    fun stopJourney(){
        endDate = LocalDate.now()
        endTime = SystemClock.elapsedRealtime()
        isPending = false
        duration = endTime - startTime
    }

    fun addLocation(location: Localisation){
        listLocalisation.add(location)
    }

    fun numberOfLocalisation(): Int {
        return listLocalisation.size
    }

    override fun toString(): String {
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val formatter = SimpleDateFormat("HH'h'mm'm'ss's'")
        val durationParsed = formatter.format(Date(duration!!))

        return "Trajet commencé le : ${startDate?.format(dateFormatter)}\n" +
                "Fini le : ${endDate?.format(dateFormatter)}\n" +
                "Il a duré : $durationParsed\n" +
                "Nombre de localisation reçu : ${numberOfLocalisation()}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isPending) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Journey> {
        override fun createFromParcel(parcel: Parcel): Journey {
            return Journey(parcel)
        }

        override fun newArray(size: Int): Array<Journey?> {
            return arrayOfNulls(size)
        }
    }
}