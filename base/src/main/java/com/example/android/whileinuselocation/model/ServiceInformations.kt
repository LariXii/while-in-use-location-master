package com.example.android.whileinuselocation.model

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import com.google.android.gms.location.LocationSettingsStates

class ServiceInformations() : Parcelable {
    private val listLocalisation: MutableList<Localisation> = mutableListOf()
    var startTime: Long = SystemClock.elapsedRealtime()
    var locationSettingsStates: LocationSettingsStates? = null
    var isSending = false
    var isFromMockProvider = false

    constructor(parcel: Parcel) : this() {
    }

    fun addLocation(location: Localisation){
        listLocalisation.add(location)
    }

    fun numberOfLocalisation(): Int {
        return listLocalisation.size
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ServiceInformations> {
        override fun createFromParcel(parcel: Parcel): ServiceInformations {
            return ServiceInformations(parcel)
        }

        override fun newArray(size: Int): Array<ServiceInformations?> {
            return arrayOfNulls(size)
        }
    }
}