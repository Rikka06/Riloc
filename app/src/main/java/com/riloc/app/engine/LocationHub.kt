package com.riloc.app.engine

import com.riloc.app.common.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Simple immutable lat/lon pair (WGS-84 decimal degrees). */
data class LatLng(val lat: Double, val lon: Double)

/**
 * Live fake-location hub. Every movement source (joystick, roam, route, map tap)
 * writes through here so the map marker and the Xposed module stay in sync.
 */
object LocationHub {

    private val _current = MutableStateFlow(LatLng(Prefs.latitude(), Prefs.longitude()))
    val current: StateFlow<LatLng> = _current.asStateFlow()

    fun update(lat: Double, lon: Double) {
        Prefs.setLocation(lat, lon)
        _current.value = LatLng(lat, lon)
    }

    fun refresh() {
        _current.value = LatLng(Prefs.latitude(), Prefs.longitude())
    }
}
