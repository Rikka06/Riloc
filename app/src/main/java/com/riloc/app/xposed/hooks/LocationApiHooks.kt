package com.riloc.app.xposed.hooks

import android.location.Location
import android.os.Build
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface

/**
 * Hooks the public `android.location.Location` getters and replaces individual
 * fields with the current spoofed values (following noobexon1/XposedFakeLocation).
 *
 * Every intercept refreshes [LocationState] from the remote preferences so
 * joystick/route/roam movements written by the manager UI are picked up live.
 * Optional fields (accuracy/altitude/speed/…) are only overridden when their
 * corresponding "use" toggle is enabled in the manager.
 */
class LocationApiHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[LocationApiHooks]"

    fun init() {
        runCatching {
            val locationClass = Class.forName("android.location.Location", false, classLoader)
            hookGetter(locationClass, "getLatitude") {
                val lat = LocationState.latitude
                if (lat.isFinite() && kotlin.math.abs(lat) > 0.1) lat else 39.9042
            }
            hookGetter(locationClass, "getLongitude") {
                val lon = LocationState.longitude
                if (lon.isFinite() && kotlin.math.abs(lon) > 0.1) lon else 116.4074
            }
            hookGetter(locationClass, "getAccuracy", enabled = { LocationState.accuracy > 0f }) { LocationState.accuracy }
            hookGetter(locationClass, "getAltitude", enabled = { LocationState.altitude != 0.0 }) { LocationState.altitude }
            hookGetter(locationClass, "getSpeed", enabled = { LocationState.speed > 0f }) { LocationState.speed }
            hookGetter(locationClass, "getVerticalAccuracyMeters", enabled = { LocationState.verticalAccuracy > 0f }) { LocationState.verticalAccuracy }
            hookGetter(locationClass, "getSpeedAccuracyMetersPerSecond", enabled = { LocationState.speedAccuracy > 0f }) { LocationState.speedAccuracy }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                hookGetter(locationClass, "getMslAltitudeMeters", enabled = { false }) { 0.0 }
                hookGetter(locationClass, "getMslAltitudeAccuracyMeters", enabled = { false }) { 0.0f }
            }
            hookGetter(locationClass, "isFromMockProvider") { false }
        }.onFailure { module.log(Log.ERROR, tag, "Location getter hooks failed: ${it.message}") }

        runCatching {
            val lmClass = Class.forName("android.location.LocationManager", false, classLoader)
            HookUtil.hookAll(module, lmClass, "getLastKnownLocation", tag) { chain ->
                val original = chain.proceed()
                LocationState.update()
                if (LocationState.isTarget(packageName)) {
                    val loc = (original as? Location) ?: Location("gps")
                    val lat = LocationState.latitude
                    val lon = LocationState.longitude
                    loc.latitude = if (lat.isFinite() && kotlin.math.abs(lat) > 0.1) lat else 39.9042
                    loc.longitude = if (lon.isFinite() && kotlin.math.abs(lon) > 0.1) lon else 116.4074
                    loc.time = System.currentTimeMillis()
                    loc
                } else {
                    original
                }
            }
        }.onFailure { module.log(Log.ERROR, tag, "LocationManager getLastKnownLocation hook failed: ${it.message}") }

        module.log(Log.INFO, tag, "Location API hooks installed for $packageName")
    }

    private fun hookGetter(
        locationClass: Class<*>,
        methodName: String,
        enabled: () -> Boolean = { true },
        spoofed: () -> Any?,
    ) {
        HookUtil.hookAll(module, locationClass, methodName, tag) { chain ->
            val original = chain.proceed()
            LocationState.update()
            if (LocationState.isPlaying && LocationState.isTarget(packageName) && enabled()) {
                spoofed()
            } else {
                original
            }
        }
    }
}
