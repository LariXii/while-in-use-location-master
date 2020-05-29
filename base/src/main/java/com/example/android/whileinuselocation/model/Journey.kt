package com.example.android.whileinuselocation.model

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.text.Editable
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

    private var listLocalisation: MutableList<Localisation> = mutableListOf()

    constructor(parcel: Parcel) : this() {
        startDate = parcel.readSerializable() as LocalDate?
        endDate = parcel.readSerializable() as LocalDate?
        startTime = parcel.readLong()
        endTime = parcel.readLong()
        duration = parcel.readValue(Long::class.java.classLoader) as? Long
        isPending = parcel.readByte() != 0.toByte()
        listLocalisation = parcel.readParcelableList(listLocalisation, Localisation::class.java.classLoader)
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

        var durationParsed = "Pas encore calculé !"
        if(duration != null){
            val formatter = SimpleDateFormat("HH'h'mm'm'ss's'")
            durationParsed = formatter.format(Date(duration!!))
        }

        return "Trajet commencé le : ${startDate?.format(dateFormatter)}\n" +
                "Fini le : ${endDate?.format(dateFormatter)}\n" +
                "Il a duré : $durationParsed\n" +
                "Nombre de localisation reçu : ${numberOfLocalisation()}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(startDate)
        parcel.writeSerializable(endDate)
        parcel.writeLong(startTime)
        parcel.writeLong(endTime)
        parcel.writeValue(duration)
        parcel.writeByte(if (isPending) 1 else 0)
        parcel.writeParcelableList(listLocalisation,flags)
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