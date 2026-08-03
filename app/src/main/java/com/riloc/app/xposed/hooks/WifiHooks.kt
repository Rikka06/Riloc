package com.riloc.app.xposed.hooks

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface

/**
 * Hides Wi-Fi-based location fingerprints while spoofing (following
 * XposedFakeLocation's AppWifiHooks / FuckLocation's WLANHooker):
 *  - `getScanResults()` returns an empty list (no APs to trilaterate from)
 *  - `getConnectionInfo()` returns a neutral fake [WifiInfo]
 */
class WifiHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[WifiHooks]"

    fun init() {
        runCatching {
            val clazz = Class.forName("android.net.wifi.WifiManager", false, classLoader)

            HookUtil.hookMethod(module, clazz, "getScanResults", tag = tag) { chain ->
                val original = chain.proceed()
                LocationState.update()
                if (LocationState.isPlaying && LocationState.isTarget(packageName) && LocationState.hideWifi) {
                    emptyList<Any>()
                } else {
                    original
                }
            }

            HookUtil.hookMethod(module, clazz, "getConnectionInfo", tag = tag) { chain ->
                val original = chain.proceed()
                LocationState.update()
                if (LocationState.isPlaying && LocationState.isTarget(packageName) && LocationState.hideWifi) {
                    fakeWifiInfo()
                } else {
                    original
                }
            }
        }.onFailure { module.log(Log.ERROR, tag, "Wifi hooks failed: ${it.message}") }
    }

    private fun fakeWifiInfo(): WifiInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            WifiInfo.Builder()
                .setBssid("02:00:00:00:00:00")
                .setSsid("AndroidAP".toByteArray())
                .setRssi(-60)
                .setNetworkId(0)
                .build()
        }.getOrNull()
    }
}
