package com.riloc.app.xposed.hooks

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface
import java.util.function.Consumer

/**
 * Hooks `android.location.LocationManager` pull-style APIs so callers receive a
 * fully-formed fake [Location] (following xPoint and XposedFakeLocation):
 *  - `getLastKnownLocation(provider)` and its `LastLocationRequest` overload
 *  - `getCurrentLocation(...)` (delivers the fake location through the consumer)
 *  - `isProviderEnabled(provider)` reports GPS/network as enabled while spoofing
 */
class LocationManagerHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[LocationManagerHooks]"

    fun init() {
        runCatching {
            val lmClass = Class.forName("android.location.LocationManager", false, classLoader)

            HookUtil.hookMethod(module, lmClass, "getLastKnownLocation", String::class.java, tag = tag) { chain ->
                val provider = chain.getArg(0) as String
                val original = chain.proceed() as? Location
                LocationState.update()
                if (active()) {
                    LocationState.createFakeLocation(original, provider)
                } else {
                    original
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val requestClass = runCatching {
                    Class.forName("android.location.LastLocationRequest", false, classLoader)
                }.getOrNull()
                if (requestClass != null) {
                    HookUtil.hookMethod(module, lmClass, "getLastKnownLocation", String::class.java, requestClass, tag = tag) { chain ->
                        val provider = chain.getArg(0) as String
                        val original = chain.proceed() as? Location
                        LocationState.update()
                        if (active()) {
                            LocationState.createFakeLocation(original, provider)
                        } else {
                            original
                        }
                    }
                }
            }

            hookCurrentLocation(lmClass)
            hookIsProviderEnabled(lmClass)
        }.onFailure { module.log(Log.ERROR, tag, "LocationManager hooks failed: ${it.message}") }
        module.log(Log.INFO, tag, "LocationManager hooks installed for $packageName")
    }

    private fun active(): Boolean {
        LocationState.update()
        return LocationState.isPlaying && LocationState.isTarget(packageName)
    }

    /**
     * Hooks every `getCurrentLocation` overload and pushes the fake location into
     * the consumer argument while suppressing the real (blocking) result.
     */
    private fun hookCurrentLocation(lmClass: Class<*>) {
        HookUtil.hookAll(module, lmClass, "getCurrentLocation", tag) { chain ->
            LocationState.update()
            if (!active()) return@hookAll chain.proceed()
            val consumer = chain.args.firstOrNull { it is Consumer<*> } as? Consumer<Location>
            if (consumer != null) {
                val fake = LocationState.createFakeLocation(null, LocationManager.FUSED_PROVIDER)
                runCatching { consumer.accept(fake) }
                null
            } else {
                chain.proceed()
            }
        }
    }

    private fun hookIsProviderEnabled(lmClass: Class<*>) {
        HookUtil.hookMethod(module, lmClass, "isProviderEnabled", String::class.java, tag = tag) { chain ->
            val result = chain.proceed()
            val provider = chain.args.getOrNull(0) as? String
            if (active() && provider in setOf("gps", "network", "fused", "passive")) true else result
        }
    }
}
