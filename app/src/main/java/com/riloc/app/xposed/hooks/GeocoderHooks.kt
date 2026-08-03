package com.riloc.app.xposed.hooks

import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface
import java.util.Locale

/**
 * Hooks [Geocoder] so reverse/forward geocoding is consistent with the spoofed
 * coordinates (following hazbu/xPoint): `isPresent()` always reports true and
 * `getFromLocation()` returns a single [Address] at the fake position.
 */
class GeocoderHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[GeocoderHooks]"

    fun init() {
        runCatching {
            val clazz = Class.forName("android.location.Geocoder", false, classLoader)

            HookUtil.hookMethod(module, clazz, "isPresent", tag = tag) { chain ->
                val original = chain.proceed()
                LocationState.update()
                if (LocationState.isPlaying && LocationState.isTarget(packageName)) true else original
            }

            HookUtil.hookMethod(
                module, clazz, "getFromLocation",
                java.lang.Double.TYPE,
                java.lang.Double.TYPE,
                java.lang.Integer.TYPE,
                tag = tag,
            ) { chain ->
                LocationState.update()
                if (!(LocationState.isPlaying && LocationState.isTarget(packageName))) {
                    return@hookMethod chain.proceed()
                }
                val addresses = ArrayList<Address>()
                val address = Address(Locale.getDefault())
                address.latitude = LocationState.latitude
                address.longitude = LocationState.longitude
                address.countryName = "China"
                addresses.add(address)
                addresses
            }
        }.onFailure { module.log(Log.ERROR, tag, "Geocoder hooks failed: ${it.message}") }
    }
}
