package com.riloc.app.xposed

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.riloc.app.common.APP_PACKAGE
import com.riloc.app.common.KEY_HIDE_TOAST
import com.riloc.app.common.REMOTE_PREFS_GROUP
import com.riloc.app.xposed.hooks.AntiDetectionHooks
import com.riloc.app.xposed.hooks.GeocoderHooks
import com.riloc.app.xposed.hooks.LocationApiHooks
import com.riloc.app.xposed.hooks.LocationManagerHooks
import com.riloc.app.xposed.hooks.MockHideHooks
import com.riloc.app.xposed.hooks.NmeaHooks
import com.riloc.app.xposed.hooks.SensorHooks
import com.riloc.app.xposed.hooks.SystemHooks
import com.riloc.app.xposed.hooks.TelephonyHooks
import com.riloc.app.xposed.hooks.VendorSdkHooks
import com.riloc.app.xposed.hooks.WifiHooks
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

private const val TAG = "[RilocModule]"

/**
 * libxposed entry point (declared in `resources/META-INF/xposed/java_init.list`).
 *
 * Installs the right hook set per process:
 *  - target apps            → location spoofing + anti-detection + geocoder + wifi/telephony hiding + vendor SDKs + NMEA + sensors
 *  - `com.android.phone`    → cellular data hiding (for apps that query it indirectly)
 *  - `system` (system_server) → deep system hooks (only when enabled in the manager)
 */
class RilocModule : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "onModuleLoaded: ${param.processName}")
        LocationState.logger = { priority, tag, message -> log(priority, tag, message) }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        log(Log.INFO, TAG, "onPackageLoaded: ${param.packageName}")
        // Remote preferences are available as soon as the package is loaded; cache them
        // so per-hook reads are cheap.
        LocationState.setPreferences(getRemotePreferences(REMOTE_PREFS_GROUP))
    }

    override fun onPackageReady(param: PackageReadyParam) {
        log(Log.INFO, TAG, "onPackageReady: ${param.packageName}")
        if (!param.isFirstPackage) return // run per-package setup only once per process
        if (param.packageName == APP_PACKAGE) return // never hook our own manager

        if (param.packageName == "com.android.phone") {
            // Cell data is queried by other apps through this process; hide it globally.
            TelephonyHooks(this, param.classLoader, param.packageName).init()
        } else {
            LocationApiHooks(this, param.classLoader, param.packageName).init()
            LocationManagerHooks(this, param.classLoader, param.packageName).init()
            MockHideHooks(this, param.classLoader, param.packageName).init()
            GeocoderHooks(this, param.classLoader, param.packageName).init()
            WifiHooks(this, param.classLoader, param.packageName).init()
            TelephonyHooks(this, param.classLoader, param.packageName).init()
            VendorSdkHooks(this, param.classLoader, param.packageName).init()
            NmeaHooks(this, param.classLoader, param.packageName).init()
            AntiDetectionHooks(this, param.classLoader, param.packageName).init()
            SensorHooks(this, param.classLoader, param.packageName).init()
            com.riloc.app.xposed.hooks.GnssStatusHooks(this, param.classLoader, param.packageName).init()
            maybeShowActiveToast(param)

        }
    }


    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(Log.INFO, TAG, "onSystemServerStarting")
        LocationState.setPreferences(getRemotePreferences(REMOTE_PREFS_GROUP))
        LocationState.update()
        if (LocationState.enableSystemHooks) {
            SystemHooks(this, param.classLoader).init()
        } else {
            log(Log.INFO, TAG, "System hooks disabled; skipping system_server hooks")
        }
    }

    /** Shows a one-shot toast in the target app unless the user disabled it. */
    private fun maybeShowActiveToast(param: PackageReadyParam) {
        val hideToast = getRemotePreferences(REMOTE_PREFS_GROUP).getBoolean(KEY_HIDE_TOAST, false)
        if (hideToast) return
        runCatching {
            val clazz = Class.forName("android.app.Instrumentation", false, param.classLoader)
            val method = clazz.getDeclaredMethod("callApplicationOnCreate", Application::class.java)
            hook(method).intercept { chain ->
                val result = chain.proceed()
                runCatching {
                    val context = (chain.getArg(0) as Application).applicationContext
                    Toast.makeText(context, "Riloc 虚拟定位已生效", Toast.LENGTH_SHORT).show()
                }
                result
            }
        }.onFailure { log(Log.ERROR, TAG, "Toast hook failed: ${it.message}") }
    }
}
