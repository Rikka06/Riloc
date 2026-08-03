package com.riloc.app.xposed.hooks

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import java.lang.reflect.Method

/**
 * Deep system-server hooks. Only active when the user enabled system hooks in the
 * manager AND added `system` (+ `com.android.phone` for telephony) to the LSPosed
 * module scope.
 *
 * Implements the strongest interception layers from the reference projects
 * (FuckLocation's LocationHookerAfterS/GnssManagerServiceHookerS and
 * XposedFakeLocation's SystemServicesHooks):
 *  - `LocationManagerService.getLastLocation` → fake location
 *  - `LocationManagerService.getCurrentLocation` → null for targets
 *  - `LocationProviderManager.onReportLocation` (S+) → per-registration fake delivery
 *  - `LocationManagerService$Receiver.callLocationChangedLocked` (pre-S) → arg replacement
 *  - GNSS callback registration → blocked for targets
 *  - `requestGeofence` → blocked for targets
 *  - MIUI blurry location → replaced/cleared
 */
class SystemHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
) {
    private val tag = "[SystemHooks]"

    fun init() {
        val lms = HookUtil.findClass(
            classLoader,
            "com.android.server.location.LocationManagerService",
            "com.android.server.LocationManagerService",
        )
        if (lms != null) {
            hookLastLocation(lms)
            hookCurrentLocation(lms)
            hookGeofence(lms)
            hookReceiverCallbacks()
        } else {
            module.log(Log.WARN, tag, "LocationManagerService not found; skipping LMS hooks")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hookLocationProviderManager()
        }
        hookGnssBlockers()
        hookMiuiBlur()
        module.log(Log.INFO, tag, "System hooks installed")
    }

    private fun isActive(): Boolean {
        LocationState.update()
        return LocationState.isPlaying && LocationState.enableSystemHooks
    }

    /** Returns true when any caller package recovered from [value] is a selected target. */
    private fun shouldSpoof(value: Any?): Boolean {
        if (!isActive()) return false
        val packages = HookUtil.collectPackageNames(value)
        return packages.any { LocationState.isTarget(it) }
    }

    // ── LocationManagerService pull APIs ──────────────────────────────

    private fun hookLastLocation(lms: Class<*>) {
        HookUtil.hookAll(module, lms, "getLastLocation", tag) { chain ->
            val result = chain.proceed()
            if (shouldSpoof(chain.args)) {
                val original = result as? Location
                LocationState.createFakeLocation(original)
            } else {
                result
            }
        }
    }

    private fun hookCurrentLocation(lms: Class<*>) {
        HookUtil.hookAll(module, lms, "getCurrentLocation", tag) { chain ->
            if (shouldSpoof(chain.args)) {
                HookUtil.defaultReturnValue(chain.executable as? Method)
            } else {
                chain.proceed()
            }
        }
    }

    private fun hookGeofence(lms: Class<*>) {
        HookUtil.hookAll(module, lms, "requestGeofence", tag) { chain ->
            if (shouldSpoof(chain.args)) {
                HookUtil.defaultReturnValue(chain.executable as? Method)
            } else {
                chain.proceed()
            }
        }
    }

    // ── Provider-level push (Android 12+) ─────────────────────────────

    private fun hookLocationProviderManager() {
        val clazz = HookUtil.findClass(classLoader, "com.android.server.location.provider.LocationProviderManager")
            ?: return
        HookUtil.hookAll(module, clazz, "onReportLocation", tag) { chain ->
            interceptOnReportLocation(chain)
        }
    }

    private fun interceptOnReportLocation(chain: Chain): Any? {
        if (!isActive()) return chain.proceed()
        val locationResult = chain.args.firstOrNull() ?: return chain.proceed()
        val providerClass = chain.thisObject?.javaClass ?: return chain.proceed()

        val registrationsField = HookUtil.findField(providerClass, "mRegistrations") ?: return chain.proceed()
        val registrations = registrationsField.get(chain.thisObject) as? Map<*, *> ?: return chain.proceed()

        val locationsField = HookUtil.findField(locationResult.javaClass, "mLocations") ?: return chain.proceed()
        val originalLocations = locationsField.get(locationResult) as? List<*> ?: return chain.proceed()
        val original = originalLocations.firstOrNull() as? Location
        val fake = LocationState.createFakeLocation(original)

        val originalRegistrations = registrations
        val passthrough = LinkedHashMap<Any, Any>()
        var delivered = false

        registrations.forEach { (key, value) ->
            val packages = HookUtil.collectPackageNames(value)
            if (packages.any { LocationState.isTarget(it) }) {
                runCatching {
                    locationsField.set(locationResult, arrayListOf(fake))
                    deliverToRegistration(value!!, locationResult)
                    delivered = true
                }.onFailure { module.log(Log.ERROR, tag, "fake delivery failed: ${it.message}") }
            } else {
                passthrough[key!! as Any] = value!! as Any
            }
        }

        locationsField.set(locationResult, ArrayList(originalLocations))
        registrationsField.set(chain.thisObject, passthrough)
        return try {
            if (delivered) {
                // Real location must not reach spoofed registrations; they already got the fake.
                if (passthrough.isEmpty()) null else chain.proceed()
            } else {
                chain.proceed()
            }
        } finally {
            registrationsField.set(chain.thisObject, originalRegistrations)
        }
    }

    private fun deliverToRegistration(registration: Any, locationResult: Any) {
        val accept = HookUtil.findMethod(registration.javaClass, "acceptLocationChange") ?: return
        val operation = accept.invoke(registration, locationResult)
        val execute = HookUtil.findMethod(registration.javaClass, "executeOperation") ?: return
        execute.invoke(registration, operation)
    }

    // ── Pre-S receiver callback (Android 8–11) ────────────────────────

    private fun hookReceiverCallbacks() {
        val receiverClass = HookUtil.findClass(
            classLoader,
            "com.android.server.location.LocationManagerService\$Receiver",
            "com.android.server.LocationManagerService\$Receiver",
        ) ?: return

        HookUtil.hookAll(module, receiverClass, "callLocationChangedLocked", tag) { chain ->
            if (!isActive()) return@hookAll chain.proceed()
            if (!shouldSpoof(chain.thisObject)) return@hookAll chain.proceed()

            val args = chain.args
            val index = args.indexOfFirst { it is Location }
            if (index == -1) return@hookAll chain.proceed()

            val newArgs = args.toTypedArray()
            newArgs[index] = LocationState.createFakeLocation(args[index] as Location)
            chain.proceed(newArgs)
        }
    }

    // ── GNSS raw-data leak blocking ───────────────────────────────────

    private fun hookGnssBlockers() {
        val classes = listOfNotNull(
            HookUtil.findClass(classLoader, "com.android.server.location.gnss.GnssManagerService"),
            HookUtil.findClass(
                classLoader,
                "com.android.server.location.LocationManagerService",
                "com.android.server.LocationManagerService",
            ),
        ).distinct()

        val methods = listOf(
            "addGnssBatchingCallback",
            "addGnssMeasurementsListener",
            "addGnssNavigationMessageListener",
            "addGnssAntennaInfoListener",
            "registerGnssStatusCallback",
            "registerGnssNmeaCallback",
        )
        classes.forEach { clazz ->
            methods.forEach { name ->
                HookUtil.hookAll(module, clazz, name, tag) { chain ->
                    if (shouldSpoof(chain.args)) {
                        HookUtil.defaultReturnValue(chain.executable as? Method)
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
    }

    // ── MIUI blurry-location workaround ───────────────────────────────

    private fun hookMiuiBlur() {
        val clazz = HookUtil.findClass(
            classLoader,
            "com.android.server.location.MiuiBlurLocationManagerImpl",
            "com.android.server.location.MiuiBlurLocationManager",
        ) ?: return

        HookUtil.hookAll(module, clazz, "getBlurryLocation", tag) { chain ->
            val result = chain.proceed()
            if (shouldSpoof(chain.args)) {
                val original = result as? Location
                LocationState.createFakeLocation(original, LocationManager.FUSED_PROVIDER)
            } else {
                result
            }
        }
        HookUtil.hookAll(module, clazz, "getBlurryCellLocation", tag) { chain ->
            val result = chain.proceed()
            if (shouldSpoof(chain.args)) null else result
        }
        HookUtil.hookAll(module, clazz, "getBlurryCellInfos", tag) { chain ->
            val result = chain.proceed()
            if (shouldSpoof(chain.args)) emptyList<Any>() else result
        }
    }
}
