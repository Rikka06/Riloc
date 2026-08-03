package com.riloc.app.xposed.hooks

import android.util.Log
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface

/**
 * Hooks third-party location SDKs commonly included in Chinese applications:
 *  - Baidu Location SDK (`com.baidu.location.BDLocation`)
 *  - AutoNavi / AMap Location SDK (`com.amap.api.location.AMapLocation`)
 *  - Tencent Location SDK (`com.tencent.map.geolocation.TencentLocation`)
 *
 * Forces getters to return spoofed coordinates, altitude, accuracy, speed, and valid location status types.
 */
class VendorSdkHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[VendorSdkHooks]"

    fun init() {
        runCatching { hookBaiduSdk() }
            .onFailure { module.log(Log.DEBUG, tag, "Baidu SDK hooks skipped or failed: ${it.message}") }

        runCatching { hookAmapSdk() }
            .onFailure { module.log(Log.DEBUG, tag, "AMap SDK hooks skipped or failed: ${it.message}") }

        runCatching { hookTencentSdk() }
            .onFailure { module.log(Log.DEBUG, tag, "Tencent SDK hooks skipped or failed: ${it.message}") }

        module.log(Log.INFO, tag, "Vendor SDK hooks initialized for $packageName")
    }

    private fun active(): Boolean {
        LocationState.update()
        return LocationState.isPlaying && LocationState.hookVendorSdks && LocationState.isTarget(packageName)
    }

    // ── Baidu Location SDK ─────────────────────────────────────────────

    private fun hookBaiduSdk() {
        val clazz = HookUtil.findClass(classLoader, "com.baidu.location.BDLocation") ?: return

        HookUtil.hookAll(module, clazz, "getLatitude", tag) { chain ->
            val original = chain.proceed()
            if (active()) LocationState.latitude else original
        }
        HookUtil.hookAll(module, clazz, "getLongitude", tag) { chain ->
            val original = chain.proceed()
            if (active()) LocationState.longitude else original
        }
        HookUtil.hookAll(module, clazz, "getLocType", tag) { chain ->
            val original = chain.proceed()
            // 61 = GPS location, 161 = Network location success in Baidu SDK
            if (active()) 61 else original
        }
        HookUtil.hookAll(module, clazz, "getRadius", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.accuracy > 0f) LocationState.accuracy else original
        }
        HookUtil.hookAll(module, clazz, "getAltitude", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.altitude != 0.0) LocationState.altitude else original
        }
        HookUtil.hookAll(module, clazz, "getSpeed", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.speed > 0f) LocationState.speed else original
        }
    }

    // ── AMap (AutoNavi) Location SDK ───────────────────────────────────

    private fun hookAmapSdk() {
        val clazz = HookUtil.findClass(classLoader, "com.amap.api.location.AMapLocation") ?: return

        HookUtil.hookAll(module, clazz, "getLatitude", tag) { chain ->
            val original = chain.proceed()
            if (active()) LocationState.latitude else original
        }
        HookUtil.hookAll(module, clazz, "getLongitude", tag) { chain ->
            val original = chain.proceed()
            if (active()) LocationState.longitude else original
        }
        HookUtil.hookAll(module, clazz, "getErrorCode", tag) { chain ->
            val original = chain.proceed()
            if (active()) 0 else original
        }
        HookUtil.hookAll(module, clazz, "getAccuracy", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.accuracy > 0f) LocationState.accuracy else original
        }
        HookUtil.hookAll(module, clazz, "getAltitude", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.altitude != 0.0) LocationState.altitude else original
        }
        HookUtil.hookAll(module, clazz, "getSpeed", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.speed > 0f) LocationState.speed else original
        }
    }

    // ── Tencent Location SDK ───────────────────────────────────────────

    private fun hookTencentSdk() {
        val clazz = HookUtil.findClass(classLoader, "com.tencent.map.geolocation.TencentLocation") ?: return

        HookUtil.hookAll(module, clazz, "getLatitude", tag) { chain ->
            val original = chain.proceed()
            if (active()) LocationState.latitude else original
        }
        HookUtil.hookAll(module, clazz, "getLongitude", tag) { chain ->
            val original = chain.proceed()
            if (active()) LocationState.longitude else original
        }
        HookUtil.hookAll(module, clazz, "getAccuracy", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.accuracy > 0f) LocationState.accuracy else original
        }
        HookUtil.hookAll(module, clazz, "getAltitude", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.altitude != 0.0) LocationState.altitude else original
        }
        HookUtil.hookAll(module, clazz, "getSpeed", tag) { chain ->
            val original = chain.proceed()
            if (active() && LocationState.speed > 0f) LocationState.speed else original
        }
    }
}
