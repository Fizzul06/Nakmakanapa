package com.spg.nakmakanapa.location

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.*

data class CityLocation(val name: String, val lat: Double, val lng: Double)

object LocationHelper {

    // Default reference locations in Malaysia
    val KUALA_LUMPUR = CityLocation("Kuala Lumpur, MY", 3.1579, 101.7118)
    val BENTONG_PAHANG = CityLocation("Bentong (Lemang To'ki), Pahang", 3.5222, 101.9125)
    val PENANG = CityLocation("Georgetown, Penang", 5.4164, 100.3327)
    val JOHOR_BAHRU = CityLocation("Johor Bahru, Johor", 1.4927, 103.7414)
    val IPOH = CityLocation("Ipoh, Perak", 4.5975, 101.0901)

    val PRESET_LOCATIONS = listOf(KUALA_LUMPUR, BENTONG_PAHANG, PENANG, JOHOR_BAHRU, IPOH)

    fun getFusedLocationClient(context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Checks if coordinates fall within Malaysia bounds
     */
    fun isLocationInMalaysia(lat: Double, lng: Double): Boolean {
        return lat in 0.8..7.5 && lng in 99.0..119.5
    }

    /**
     * Calculates distance between two coordinates in kilometers using Haversine formula
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}