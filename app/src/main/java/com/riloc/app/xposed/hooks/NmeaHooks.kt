package com.riloc.app.xposed.hooks

import android.location.LocationManager
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface

/**
 * Intercepts NMEA listeners on [LocationManager] (following noobexon1/XposedFakeLocation and FuckLocation).
 *
 * Raw NMEA sentences contain satellite telemetry and true coordinates. When location spoofing is active,
 * registering NMEA listeners is either blocked or NMEA string notifications are silenced so raw satellite
 * data cannot leak true GPS positions.
 */
class NmeaHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[NmeaHooks]"

    fun init() {
        runCatching {
            val lmClass = Class.forName("android.location.LocationManager", false, classLoader)

            val nmeaMethods = listOf(
                "addNmeaListener",
                "removeNmeaListener",
                "registerGnssNmeaCallback",
                "unregisterGnssNmeaCallback",
            )

            nmeaMethods.forEach { methodName ->
                HookUtil.hookAll(module, lmClass, methodName, tag) { chain ->
                    LocationState.update()
                    if (LocationState.isPlaying && LocationState.hookNmea && LocationState.isTarget(packageName)) {
                        // Return true or false indicating success depending on return type, suppressing registration
                        val returnType = (chain.executable as? java.lang.reflect.Method)?.returnType
                        if (returnType == Boolean::class.javaPrimitiveType) true else null
                    } else {
                        chain.proceed()
                    }
                }
            }
        }.onFailure { module.log(Log.ERROR, tag, "NMEA hooks failed: ${it.message}") }

        module.log(Log.INFO, tag, "NMEA hooks installed for $packageName")
    }
}
