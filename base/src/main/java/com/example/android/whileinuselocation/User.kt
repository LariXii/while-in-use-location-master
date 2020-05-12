package com.example.android.whileinuselocation

import android.location.Location

class User(val firstName: String){
    private val listLocalisation: MutableList<Localisation> = mutableListOf()
    fun addLocation(location: Localisation){
        listLocalisation.add(location)
    }

    fun numberOfLocalisation(): Int {
        return listLocalisation.size
    }
}