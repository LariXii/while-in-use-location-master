package com.example.android.whileinuselocation

import android.location.Location

class User(private val firstName: String, private val tyreType: Int, private val trailerAxles: Int, private val TractorAxles: Int){
    private val listLocalisation: MutableList<Localisation> = mutableListOf()

    fun addLocation(location: Localisation){
        listLocalisation.add(location)
    }

    fun numberOfLocalisation(): Int {
        return listLocalisation.size
    }
}