package com.example.android.whileinuselocation.model

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Localisation(private val _sequenceNumber: Int, private val location: Location?, private val _tyreType: Int, private val _tractorAxles: Int, private val _trailerAxles: Int): Parcelable{

    //Identifiant de l'équipement
    private val c1: Int = 399367311

    //identifiant du frabricant
    private val c2: Int = 3

    //Identifiant du fournisseur de contrat
    private val c3: Int = 10747906

    //Identifiant du type de pneu
    private val c4: Int = _tyreType

    //Nombre d'essieu de remorque
    private val c5: Int = _trailerAxles

    //Nombre d'essieu du tracteur
    private val c6: Int = _tractorAxles

    //Temps auquel est arrivé la localisation
    private val c7: String

    //Latitude
    private val c8: Double

    //Longitude
    private val c9: Double

    //Vitesse instantané
    private val c10: Float

    //Direction de la localisation
    private val c11: Float

    //Précision de la localisation
    private val c12: Float

    //Type de localisation
    private val c13: Int = 20

    //Numéro de séquence de la localisation
    private val c14: Int = _sequenceNumber

    //Altitude
    private val c15: Double

    //Poids maximal que le camion peut supporter
    private val c16: String = ""

    //Code du pays
    private val c17: String = "BE"

    //Identifiant du fournisseur du domaine de péage
    private val c18: Int = 16383

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readParcelable(Location::class.java.classLoader),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    init{
        //Calcul de la date réel de la localisation
        val toSecond = 1000000000
        //Temps en nanosecondes du fix
        val time = location?.elapsedRealtimeNanos?.div(toSecond)
        //Temps en nanosecondes du système au momemt de la réception du fix
        val sTime = SystemClock.elapsedRealtimeNanos()/toSecond
        val ageTime = sTime - time!!

        val date = LocalDateTime.now().minusSeconds(ageTime)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        c7 = date.format(formatter)

        if(location != null){
            c8 = location.latitude
            c9 = location.longitude
            c10 = location.speed
            c11 = location.bearing
            c12 = location.accuracy/5
            c15 = location.altitude
        }
        else{
            throw ClassFormatError("Une location est requise")
        }
    }

    override fun toString(): String {
        val speed = location?.speed
        val speedAccuracyMetersPerSecond = location?.speedAccuracyMetersPerSecond

        val bearing = location?.bearing
        val bearingAccuracyDegrees = location?.bearingAccuracyDegrees

        val toSecond = 1000000000
        val time = location?.elapsedRealtimeNanos?.div(toSecond)
        val sTime = SystemClock.elapsedRealtimeNanos()/toSecond

        val date = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val parsedDate = date.format(formatter)

        //val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        //val currentDateandTime: String = sdf.format(Date())

        return if (location != null) {
            "ID : $c14\nLati : ${location.latitude}\nLong : ${location.longitude}\nPrécision : ${location.accuracy/5}\nCap : $bearing ± $bearingAccuracyDegrees\nDate : $parsedDate\nÂge du fix : ${sTime- time!!}s\nVitesse : $speed ± $speedAccuracyMetersPerSecond\n"
        } else{
            "No location"
        }
    }

    fun toMAPM(): String{
        return "$c1;$c2;$c3;$c4;$c5;$c6;\"$c7\";$c8;$c9;$c10;$c11;$c12;$c13;$c14;$c15;$c16;\"$c17\";$c18;\r\n"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(_sequenceNumber)
        parcel.writeParcelable(location, flags)
        parcel.writeInt(_tyreType)
        parcel.writeInt(_tractorAxles)
        parcel.writeInt(_trailerAxles)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Localisation> {
        override fun createFromParcel(parcel: Parcel): Localisation {
            return Localisation(
                parcel
            )
        }

        override fun newArray(size: Int): Array<Localisation?> {
            return arrayOfNulls(size)
        }
    }


}