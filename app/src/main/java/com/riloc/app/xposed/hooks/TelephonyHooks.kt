package com.riloc.app.xposed.hooks

import android.telephony.CellInfo
import android.telephony.TelephonyManager
import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface

/**
 * Hides cellular location fingerprints in target app processes (following
 * FuckLocation's TelephonyRegistryHooker / PhoneInterfaceManagerHooker):
 *  - `getAllCellInfo()` returns an empty list
 *  - `getCellLocation()` returns null
 *  - `getNetworkOperator()` returns an unknown MCC-MNC
 */
class TelephonyHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[TelephonyHooks]"

    fun init() {
        runCatching {
            val clazz = Class.forName("android.telephony.TelephonyManager", false, classLoader)

            HookUtil.hookMethod(module, clazz, "getAllCellInfo", tag = tag) { chain ->
                val original = chain.proceed()
                LocationState.update()
                if (LocationState.isPlaying && LocationState.isTarget(packageName) && LocationState.hideTelephony) {
                    emptyList<CellInfo>()
                } else {
                    original
                }
            }

            HookUtil.hookMethod(module, clazz, "getCellLocation", tag = tag) { chain ->
                val original = chain.proceed()
                LocationState.update()
                if (LocationState.isPlaying && LocationState.isTarget(packageName) && LocationState.hideTelephony) {
                    null
                } else {
                    original
                }
            }

            HookUtil.hookMethod(module, clazz, "getNetworkOperator", tag = tag) { chain ->
                val original = chain.proceed()
                LocationState.update()
                if (LocationState.isPlaying && LocationState.isTarget(packageName) && LocationState.hideTelephony) {
                    "00000"
                } else {
                    original
                }
            }
        }.onFailure { module.log(Log.ERROR, tag, "Telephony hooks failed: ${it.message}") }
    }
}
