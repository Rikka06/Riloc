package com.riloc.app.engine

import kotlin.math.*

/** Minimal spherical-earth helpers used by the movement engine. */
object GeoMath {

    const val EARTH_RADIUS_M = 6371000.0

    /** Haversine distance in metres. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    /** Initial bearing (degrees, 0 = north, clockwise) from point 1 to point 2. */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1r = Math.toRadians(lat1)
        val lat2r = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(lat2r)
        val x = cos(lat1r) * sin(lat2r) - sin(lat1r) * cos(lat2r) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Destination point given start, bearing (degrees) and distance (metres). */
    fun destination(lat: Double, lon: Double, bearingDeg: Double, distanceM: Double): Pair<Double, Double> {
        val lat1r = Math.toRadians(lat)
        val lon1r = Math.toRadians(lon)
        val br = Math.toRadians(bearingDeg)
        val d = distanceM / EARTH_RADIUS_M
        val sinLat = sin(lat1r) * cos(d) + cos(lat1r) * sin(d) * cos(br)
        val newLat = asin(sinLat)
        val newLon = lon1r + atan2(sin(br) * sin(d) * cos(lat1r), cos(d) - sin(lat1r) * sinLat)
        return Math.toDegrees(newLat).coerceIn(-90.0, 90.0) to
            ((Math.toDegrees(newLon) + 540.0) % 360.0 - 180.0)
    }

    /** Linear interpolation between two points (t in 0..1). */
    fun interpolate(lat1: Double, lon1: Double, lat2: Double, lon2: Double, t: Double): Pair<Double, Double> =
        (lat1 + (lat2 - lat1) * t) to (lon1 + (lon2 - lon1) * t)
}
