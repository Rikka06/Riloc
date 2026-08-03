package com.riloc.app.xposed.hooks

import android.app.AppOpsManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import com.riloc.app.common.KEY_HIDE_APP_OPS
import com.riloc.app.common.KEY_HIDE_MOCK_FLAG
import com.riloc.app.common.KEY_HIDE_SETTINGS
import com.riloc.app.common.KEY_NORMALIZE_PROVIDER
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface

/**
 * Anti-detection hooks that make spoofed locations indistinguishable from real ones.
 *
 * Techniques (from auag0/HideMockLocation and noobexon1/XposedFakeLocation):
 *  - `Location.isMock()` / `isFromMockProvider()` return false
 *  - `setIsFromMockProvider` / `setMock` arguments are forced to false
 *  - `getExtras()`/`setExtras()` strip the `mockLocation` bundle key
 *  - after `Location.set()`, the `HAS_MOCK_PROVIDER_MASK` bit of `mFieldsMask` is cleared
 *  - `getProvider()` normalizes unknown (mock) provider names to a real one
 *  - `Settings.Secure.getStringForUser("mock_location")` returns "0"
 *  - `AppOpsManager.checkOp*` returns MODE_ERRORED for OP_MOCK_LOCATION
 */
class MockHideHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[MockHideHooks]"

    fun init() {
        runCatching {
            val locationClass = Class.forName("android.location.Location", false, classLoader)
            hookMockFlagMethods(locationClass)
            hookExtras(locationClass)
            hookSetMethod(locationClass)
            hookProvider(locationClass)
        }.onFailure { module.log(Log.ERROR, tag, "Location hooks failed: ${it.message}") }

        runCatching { hookSettingsSecure() }
            .onFailure { module.log(Log.ERROR, tag, "Settings.Secure hooks failed: ${it.message}") }

        runCatching { hookAppOps() }
            .onFailure { module.log(Log.ERROR, tag, "AppOps hooks failed: ${it.message}") }

        module.log(Log.INFO, tag, "Anti-detection hooks installed for $packageName")
    }

    private fun shouldHide(): Boolean {
        LocationState.update()
        return LocationState.isPlaying &&
            LocationState.hideMockFlag &&
            LocationState.isTarget(packageName)
    }

    // ── Location mock flag ────────────────────────────────────────────

    private fun hookMockFlagMethods(locationClass: Class<*>) {
        listOf("isMock", "isFromMockProvider").forEach { name ->
            HookUtil.hookAll(module, locationClass, name, tag) { chain ->
                val original = chain.proceed()
                if (shouldHide()) false else original
            }
        }
        listOf("setIsFromMockProvider", "setMock").forEach { name ->
            HookUtil.hookAll(module, locationClass, name, tag) { chain ->
                if (shouldHide()) {
                    val args = chain.args.toTypedArray()
                    args[0] = false
                    chain.proceed(args)
                } else {
                    chain.proceed()
                }
            }
        }
    }

    private fun hookExtras(locationClass: Class<*>) {
        HookUtil.hookAll(module, locationClass, "getExtras", tag) { chain ->
            val original = chain.proceed() as? Bundle
            if (shouldHide()) stripMockExtra(original) else original
        }
        HookUtil.hookAll(module, locationClass, "setExtras", tag) { chain ->
            val args = chain.args.toTypedArray()
            if (shouldHide() && args.size > 0 && args[0] is Bundle) {
                args[0] = stripMockExtra(args[0] as Bundle)
            }
            chain.proceed(args)
        }
    }

    private fun stripMockExtra(bundle: Bundle?): Bundle? {
        if (bundle?.containsKey("mockLocation") == true) {
            return Bundle(bundle).apply { remove("mockLocation") }
        }
        return bundle
    }

    /** After Location.set(Location), clear the HAS_MOCK_PROVIDER_MASK field bit. */
    private fun hookSetMethod(locationClass: Class<*>) {
        HookUtil.hookAll(module, locationClass, "set", tag) { chain ->
            val result = chain.proceed()
            if (shouldHide()) {
                runCatching {
                    val maskField = locationClass.getDeclaredField("HAS_MOCK_PROVIDER_MASK").apply { isAccessible = true }
                    val fieldsMaskField = locationClass.getDeclaredField("mFieldsMask").apply { isAccessible = true }
                    val mask = maskField.getInt(null)
                    var value = fieldsMaskField.getLong(chain.thisObject as Location)
                    fieldsMaskField.setLong(chain.thisObject as Location, value and mask.inv().toLong())
                }
                runCatching {
                    val extras = (chain.thisObject as Location).extras
                    if (extras?.containsKey("mockLocation") == true) {
                        (chain.thisObject as Location).extras = stripMockExtra(extras)
                    }
                }
            }
            result
        }
    }

    /** Normalize unknown provider names (mock providers) to "gps". */
    private fun hookProvider(locationClass: Class<*>) {
        val knownProviders = setOf("gps", "network", "passive", "fused")
        HookUtil.hookAll(module, locationClass, "getProvider", tag) { chain ->
            val original = chain.proceed() as? String ?: return@hookAll null
            if (shouldHide() && LocationState.normalizeProvider && original !in knownProviders) {
                "gps"
            } else {
                original
            }
        }
    }

    // ── Settings.Secure ───────────────────────────────────────────────

    private fun hookSettingsSecure() {
        val clazz = HookUtil.findClass(classLoader, "android.provider.Settings\$Secure") ?: return
        HookUtil.hookMethod(
            module, clazz, "getStringForUser",
            android.content.ContentResolver::class.java, String::class.java, java.lang.Integer.TYPE,
            tag = tag,
        ) { chain ->
            val result = chain.proceed()
            val name = chain.args.getOrNull(1) as? String
            if (name == "mock_location" && LocationState.isPlaying && LocationState.isTarget(packageName)) {
                "0"
            } else {
                result
            }
        }
    }

    // ── AppOpsManager ─────────────────────────────────────────────────

    private fun hookAppOps() {
        val clazz = HookUtil.findClass(classLoader, "android.app.AppOpsManager") ?: return
        val checkMethods = listOf(
            "checkOp", "checkOpNoThrow", "unsafeCheckOp", "unsafeCheckOpNoThrow",
        )
        checkMethods.forEach { name ->
            HookUtil.hookAll(module, clazz, name, tag) { chain ->
                val result = chain.proceed()
                LocationState.update()
                val enabled = LocationState.isPlaying &&
                    LocationState.isTarget(packageName) &&
                    LocationState.hideAppOps
                if (enabled && isMockLocationOp(chain.args.firstOrNull())) {
                    AppOpsManager.MODE_ERRORED
                } else {
                    result
                }
            }
        }
    }

    private fun isMockLocationOp(op: Any?): Boolean = when (op) {
        is String -> op == AppOpsManager.OPSTR_MOCK_LOCATION
        is Int -> op == 58 // AppOpsManager.OP_MOCK_LOCATION
        else -> false
    }
}
