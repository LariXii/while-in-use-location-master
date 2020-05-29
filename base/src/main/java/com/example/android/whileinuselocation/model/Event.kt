package com.example.android.whileinuselocation.model

class Event(_id: Int, _paramA: Int? = null, _paramB: Int? = null, _paramC: Int? = null) {
    private val evntId = _id
    private val evntParamA = _paramA
    private val evntParamB = _paramB
    private val evntParamC = _paramC
    private val evntName: String

    init{
        val name: String
        when(_id){
            11000 -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
                name = "GPS no communication"
            }
            11001 -> {
                assert(_paramA != null)
                assert(_paramB != null)
                assert(_paramC == null)
                name = "GPS interference"
            }
            11003 -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
                name = "GPS no fix"
            }
            11004 -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
                name = "GPS no fix persistent"
            }
            11005 -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
                name = "GPS location from mock provider"
            }
            else -> {
                throw ClassFormatError("Event not handled")
            }
        }
        evntName = name
    }

    override fun toString(): String {
        return "$evntId;\"$evntName\";${evntParamA?:""};${evntParamB?:""};${evntParamC?:""}"
    }

    companion object{
        const val EVNT_GPS_NO_COMMUNICATION = 11000
        const val EVNT_GPS_INTERFERENCE = 11001
        const val EVNT_GPS_NO_FIX = 11003
        const val EVNT_GPS_NO_FIX_PERSISTENT = 11004
        const val EVNT_GPS_FROM_MOCK_PROVIDER = 11005
    }
}