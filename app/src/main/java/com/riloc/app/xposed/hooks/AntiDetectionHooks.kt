package com.riloc.app.xposed.hooks

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.Log
import com.riloc.app.common.APP_PACKAGE
import com.riloc.app.xposed.LocationState
import com.riloc.app.xposed.util.HookUtil
import io.github.libxposed.api.XposedInterface

/**
 * Advanced environment and anti-detection hooks (from HideMockLocation and com.huaMax):
 *  - Package Concealment: Hides Riloc, LSPosed Manager, Magisk, and Xposed installers from target apps querying `PackageManager`.
 *  - StackTrace Cleansing: Strips Xposed/LSPosed class names from `Throwable.getStackTrace()` and `Thread.getStackTrace()`.
 *  - Development Environment Concealment: Hides `DEVELOPMENT_SETTINGS_ENABLED` and `ADB_ENABLED` in `Settings.Global`.
 */
class AntiDetectionHooks(
    private val module: XposedInterface,
    private val classLoader: ClassLoader,
    private val packageName: String,
) {
    private val tag = "[AntiDetectionHooks]"

    private val sensitivePackages = setOf(
        APP_PACKAGE,
        "org.lsposed.manager",
        "com.topjohnwu.magisk",
        "de.robv.android.xposed.installer",
        "io.github.rereturn.lsposed",
        "org.meowcat.edxposed.manager",
    )

    private val xposedStackKeywords = listOf(
        "de.robv.android.xposed",
        "io.github.libxposed",
        "org.lsposed",
        "com.riloc.app.xposed",
    )

    fun init() {
        runCatching { hookPackageManager() }
            .onFailure { module.log(Log.ERROR, tag, "Package manager anti-detect failed: ${it.message}") }

        runCatching { hookStackTrace() }
            .onFailure { module.log(Log.ERROR, tag, "StackTrace anti-detect failed: ${it.message}") }

        runCatching { hookSettingsGlobal() }
            .onFailure { module.log(Log.ERROR, tag, "Settings.Global anti-detect failed: ${it.message}") }

        module.log(Log.INFO, tag, "Anti-detection environment hooks installed for $packageName")
    }

    private fun active(): Boolean {
        LocationState.update()
        return LocationState.isPlaying && LocationState.isTarget(packageName)
    }

    // ── Package Manager Concealment ────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun hookPackageManager() {
        val pmClass = HookUtil.findClass(classLoader, "android.app.ApplicationPackageManager") ?: return

        HookUtil.hookAll(module, pmClass, "getInstalledPackages", tag) { chain ->
            val result = chain.proceed()
            if (active() && LocationState.hidePackages && result is List<*>) {
                (result as List<PackageInfo>).filterNot { it.packageName in sensitivePackages }
            } else {
                result
            }
        }

        HookUtil.hookAll(module, pmClass, "getInstalledApplications", tag) { chain ->
            val result = chain.proceed()
            if (active() && LocationState.hidePackages && result is List<*>) {
                (result as List<ApplicationInfo>).filterNot { it.packageName in sensitivePackages }
            } else {
                result
            }
        }

        HookUtil.hookAll(module, pmClass, "getPackageInfo", tag) { chain ->
            val requestedPkg = chain.args.firstOrNull() as? String
            if (active() && LocationState.hidePackages && requestedPkg in sensitivePackages) {
                throw android.content.pm.PackageManager.NameNotFoundException(requestedPkg)
            } else {
                chain.proceed()
            }
        }

        HookUtil.hookAll(module, pmClass, "getApplicationInfo", tag) { chain ->
            val requestedPkg = chain.args.firstOrNull() as? String
            if (active() && LocationState.hidePackages && requestedPkg in sensitivePackages) {
                throw android.content.pm.PackageManager.NameNotFoundException(requestedPkg)
            } else {
                chain.proceed()
            }
        }
    }

    // ── StackTrace Cleansing ──────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun hookStackTrace() {
        HookUtil.hookAll(module, Throwable::class.java, "getStackTrace", tag) { chain ->
            val elements = chain.proceed() as? Array<StackTraceElement>

            if (active() && LocationState.hideStackTrace && elements != null) {
                elements.filterNot { element ->
                    xposedStackKeywords.any { keyword -> element.className.contains(keyword) }
                }.toTypedArray()
            } else {
                elements
            }
        }
    }

    // ── Settings.Global Concealment ───────────────────────────────────

    private fun hookSettingsGlobal() {
        val clazzGlobal = HookUtil.findClass(classLoader, "android.provider.Settings\$Global")
        if (clazzGlobal != null) {
            HookUtil.hookAll(module, clazzGlobal, "getString", tag) { chain ->
                val result = chain.proceed()
                val name = chain.args.getOrNull(1) as? String
                if (active() && LocationState.hideSettings && name in setOf("development_settings_enabled", "adb_enabled")) {
                    "0"
                } else {
                    result
                }
            }
        }

        val clazzSecure = HookUtil.findClass(classLoader, "android.provider.Settings\$Secure")
        if (clazzSecure != null) {
            HookUtil.hookAll(module, clazzSecure, "getString", tag) { chain ->
                val result = chain.proceed()
                val name = chain.args.getOrNull(1) as? String
                if (active() && LocationState.hideSettings && name in setOf("mock_location", "allow_mock_location")) {
                    "0"
                } else {
                    result
                }
            }
        }
    }

}
