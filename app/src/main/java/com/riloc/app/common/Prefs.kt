package com.riloc.app.common

import android.content.Context
import android.content.SharedPreferences
import com.riloc.app.KernelSUApplication

/**
 * Single access point for settings shared between the manager UI and the Xposed module.
 *
 * Reads/writes go to the LSPosed remote preference group when the service is bound
 * (the module sees them immediately); otherwise they fall back to a local preference
 * file so the UI keeps working without LSPosed.
 *
 * Doubles are stored as raw long bits because [SharedPreferences] has no `putDouble`.
 */
object Prefs {

    private lateinit var appContext: Context
    private val local: SharedPreferences by lazy {
        appContext.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
    }

    /** Remote prefs shared with the Xposed module, or null when LSPosed is not available. */
    val remote: SharedPreferences?
        get() = KernelSUApplication.service?.getRemotePreferences(REMOTE_PREFS_GROUP)

    private val active: SharedPreferences get() = remote ?: local

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ── generic helpers ───────────────────────────────────────────────

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        active.edit().apply { block() }.apply()
    }

    // ── playing state ─────────────────────────────────────────────────

    private const val TMP_CONF_PATH = "/data/local/tmp/riloc_loc.conf"

    fun writeTmpConfigFile(gcjLat: Double, gcjLon: Double, playing: Boolean) {
        runCatching {
            val (wgsLat, wgsLon) = CoordinateConverter.gcj02ToWgs84(gcjLat, gcjLon)
            val line = "%.6f,%.6f,%.6f,%.6f,%d".format(
                java.util.Locale.US, gcjLat, gcjLon, wgsLat, wgsLon, if (playing) 1 else 0
            )
            val file = java.io.File(TMP_CONF_PATH)
            file.writeText(line)
            file.setReadable(true, false)
            file.setWritable(true, false)
        }
    }

    fun isPlaying(): Boolean = active.getBoolean(KEY_IS_PLAYING, false)
    fun setPlaying(value: Boolean) {
        edit { putBoolean(KEY_IS_PLAYING, value) }
        writeTmpConfigFile(latitude(), longitude(), value)
    }

    // ── fake coordinates ──────────────────────────────────────────────

    fun latitude(): Double = active.getLong(KEY_LATITUDE, DEFAULT_LATITUDE.toRawBits())
        .let { Double.fromBits(it) }
        .takeIf { it.isFinite() && kotlin.math.abs(it) > 0.001 } ?: DEFAULT_LATITUDE

    fun longitude(): Double = active.getLong(KEY_LONGITUDE, DEFAULT_LONGITUDE.toRawBits())
        .let { Double.fromBits(it) }
        .takeIf { it.isFinite() && kotlin.math.abs(it) > 0.001 } ?: DEFAULT_LONGITUDE


    fun setLocation(lat: Double, lon: Double) {
        edit {
            putLong(KEY_LATITUDE, lat.toRawBits())
            putLong(KEY_LONGITUDE, lon.toRawBits())
        }
        writeTmpConfigFile(lat, lon, isPlaying())
    }

    // ── location parameters ───────────────────────────────────────────

    fun useAccuracy(): Boolean = active.getBoolean(KEY_USE_ACCURACY, true)
    fun accuracy(): Double = active.getLong(KEY_ACCURACY, DEFAULT_ACCURACY.toRawBits())
        .let { Double.fromBits(it) }.takeIf { it > 0 } ?: DEFAULT_ACCURACY
    fun setAccuracy(v: Double) = edit { putBoolean(KEY_USE_ACCURACY, true); putLong(KEY_ACCURACY, v.toRawBits()) }
    fun setUseAccuracy(v: Boolean) = edit { putBoolean(KEY_USE_ACCURACY, v) }

    fun useAltitude(): Boolean = active.getBoolean(KEY_USE_ALTITUDE, false)
    fun altitude(): Double = active.getLong(KEY_ALTITUDE, DEFAULT_ALTITUDE.toRawBits())
        .let { Double.fromBits(it) }.takeIf { it >= 0 } ?: DEFAULT_ALTITUDE
    fun setAltitude(v: Double) = edit { putBoolean(KEY_USE_ALTITUDE, true); putLong(KEY_ALTITUDE, v.toRawBits()) }
    fun setUseAltitude(v: Boolean) = edit { putBoolean(KEY_USE_ALTITUDE, v) }

    fun useSpeed(): Boolean = active.getBoolean(KEY_USE_SPEED, false)
    fun speed(): Float = active.getFloat(KEY_SPEED, DEFAULT_SPEED)
    fun setSpeed(v: Float) = edit { putBoolean(KEY_USE_SPEED, true); putFloat(KEY_SPEED, v) }
    fun setUseSpeed(v: Boolean) = edit { putBoolean(KEY_USE_SPEED, v) }

    /** Writes the current movement speed without enabling the speed override. */
    fun writeSpeed(v: Float) = edit { putFloat(KEY_SPEED, v) }

    fun useVerticalAccuracy(): Boolean = active.getBoolean(KEY_USE_VERTICAL_ACCURACY, false)
    fun verticalAccuracy(): Float = active.getFloat(KEY_VERTICAL_ACCURACY, DEFAULT_VERTICAL_ACCURACY)
    fun setVerticalAccuracy(v: Float) = edit { putBoolean(KEY_USE_VERTICAL_ACCURACY, true); putFloat(KEY_VERTICAL_ACCURACY, v) }
    fun setUseVerticalAccuracy(v: Boolean) = edit { putBoolean(KEY_USE_VERTICAL_ACCURACY, v) }

    fun useSpeedAccuracy(): Boolean = active.getBoolean(KEY_USE_SPEED_ACCURACY, false)
    fun speedAccuracy(): Float = active.getFloat(KEY_SPEED_ACCURACY, DEFAULT_SPEED_ACCURACY)
    fun setSpeedAccuracy(v: Float) = edit { putBoolean(KEY_USE_SPEED_ACCURACY, true); putFloat(KEY_SPEED_ACCURACY, v) }
    fun setUseSpeedAccuracy(v: Boolean) = edit { putBoolean(KEY_USE_SPEED_ACCURACY, v) }

    fun useRandomize(): Boolean = active.getBoolean(KEY_USE_RANDOMIZE, false)
    fun randomizeRadius(): Double = active.getLong(KEY_RANDOMIZE_RADIUS, DEFAULT_RANDOMIZE_RADIUS.toRawBits())
        .let { Double.fromBits(it) }.takeIf { it > 0 } ?: DEFAULT_RANDOMIZE_RADIUS
    fun setRandomize(v: Boolean, radius: Double = randomizeRadius()) =
        edit { putBoolean(KEY_USE_RANDOMIZE, v); putLong(KEY_RANDOMIZE_RADIUS, radius.toRawBits()) }

    // ── anti-detection toggles ────────────────────────────────────────

    fun hideMockFlag(): Boolean = active.getBoolean(KEY_HIDE_MOCK_FLAG, true)
    fun setHideMockFlag(v: Boolean) = edit { putBoolean(KEY_HIDE_MOCK_FLAG, v) }

    fun normalizeProvider(): Boolean = active.getBoolean(KEY_NORMALIZE_PROVIDER, true)
    fun setNormalizeProvider(v: Boolean) = edit { putBoolean(KEY_NORMALIZE_PROVIDER, v) }

    fun hideAppOps(): Boolean = active.getBoolean(KEY_HIDE_APP_OPS, true)
    fun setHideAppOps(v: Boolean) = edit { putBoolean(KEY_HIDE_APP_OPS, v) }

    fun hideSettings(): Boolean = active.getBoolean(KEY_HIDE_SETTINGS, true)
    fun setHideSettings(v: Boolean) = edit { putBoolean(KEY_HIDE_SETTINGS, v) }

    fun hideWifi(): Boolean = active.getBoolean(KEY_HIDE_WIFI, false)
    fun setHideWifi(v: Boolean) = edit { putBoolean(KEY_HIDE_WIFI, v) }

    fun hideTelephony(): Boolean = active.getBoolean(KEY_HIDE_TELEPHONY, false)
    fun setHideTelephony(v: Boolean) = edit { putBoolean(KEY_HIDE_TELEPHONY, v) }

    fun enableSystemHooks(): Boolean = active.getBoolean(KEY_ENABLE_SYSTEM_HOOKS, false)
    fun setEnableSystemHooks(v: Boolean) = edit { putBoolean(KEY_ENABLE_SYSTEM_HOOKS, v) }

    fun hideToast(): Boolean = active.getBoolean(KEY_HIDE_TOAST, false)
    fun setHideToast(v: Boolean) = edit { putBoolean(KEY_HIDE_TOAST, v) }

    fun hookVendorSdks(): Boolean = active.getBoolean(KEY_HOOK_VENDOR_SDKS, true)
    fun setHookVendorSdks(v: Boolean) = edit { putBoolean(KEY_HOOK_VENDOR_SDKS, v) }

    fun hookNmea(): Boolean = active.getBoolean(KEY_HOOK_NMEA, true)
    fun setHookNmea(v: Boolean) = edit { putBoolean(KEY_HOOK_NMEA, v) }

    fun hidePackages(): Boolean = active.getBoolean(KEY_HIDE_PACKAGES, true)
    fun setHidePackages(v: Boolean) = edit { putBoolean(KEY_HIDE_PACKAGES, v) }

    fun hideStackTrace(): Boolean = active.getBoolean(KEY_HIDE_STACK_TRACE, true)
    fun setHideStackTrace(v: Boolean) = edit { putBoolean(KEY_HIDE_STACK_TRACE, v) }

    fun simulateSensors(): Boolean = active.getBoolean(KEY_SIMULATE_SENSORS, false)
    fun setSimulateSensors(v: Boolean) = edit { putBoolean(KEY_SIMULATE_SENSORS, v) }

    fun floatingJoystick(): Boolean = active.getBoolean(KEY_FLOATING_JOYSTICK, false)
    fun setFloatingJoystick(v: Boolean) = edit { putBoolean(KEY_FLOATING_JOYSTICK, v) }


    // ── target apps ───────────────────────────────────────────────────

    fun targetApps(): Set<String> = active.getString(KEY_TARGET_APPS, null)
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet() ?: emptySet()

    fun setTargetApps(apps: Set<String>) = edit { putString(KEY_TARGET_APPS, apps.joinToString(",")) }

    // ── UI-only local settings (not shared with the module) ────────────

    private val ui: SharedPreferences by lazy {
        appContext.getSharedPreferences("riloc_ui", Context.MODE_PRIVATE)
    }

    fun uiFloat(key: String, default: Float): Float = ui.getFloat(key, default)
    fun setUiFloat(key: String, value: Float) = ui.edit().putFloat(key, value).apply()
    fun uiDouble(key: String, default: Double): Double = ui.getLong(key, default.toRawBits()).let { Double.fromBits(it) }
    fun setUiDouble(key: String, value: Double) = ui.edit().putLong(key, value.toRawBits()).apply()
    fun uiInt(key: String, default: Int): Int = ui.getInt(key, default)
    fun setUiInt(key: String, value: Int) = ui.edit().putInt(key, value).apply()
    fun uiBool(key: String, default: Boolean): Boolean = ui.getBoolean(key, default)
    fun setUiBool(key: String, value: Boolean) = ui.edit().putBoolean(key, value).apply()
    fun uiString(key: String, default: String?): String? = ui.getString(key, default)
    fun setUiString(key: String, value: String?) = ui.edit().putString(key, value).apply()

    // ── AMap API Key settings (Shadow & GlobalTraveling) ───────────────
    private const val DEFAULT_AMAP_KEY = "8325164e247e15eea68b59e89200988b"
    fun amapKey(): String = uiString("amap_web_key", DEFAULT_AMAP_KEY) ?: DEFAULT_AMAP_KEY
    fun setAmapKey(key: String) = setUiString("amap_web_key", key.ifBlank { DEFAULT_AMAP_KEY })
    fun amapSecret(): String = uiString("amap_secret_key", "") ?: ""
    fun setAmapSecret(sec: String) = setUiString("amap_secret_key", sec)
}

