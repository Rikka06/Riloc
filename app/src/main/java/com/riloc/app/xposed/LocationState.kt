package com.riloc.app.xposed

import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.riloc.app.common.DEFAULT_LATITUDE
import com.riloc.app.common.DEFAULT_LONGITUDE
import com.riloc.app.common.CoordinateConverter
import com.riloc.app.common.KEY_ACCURACY
import com.riloc.app.common.KEY_ALTITUDE
import com.riloc.app.common.KEY_ENABLE_SYSTEM_HOOKS
import com.riloc.app.common.KEY_HIDE_APP_OPS
import com.riloc.app.common.KEY_HIDE_MOCK_FLAG
import com.riloc.app.common.KEY_HIDE_SETTINGS
import com.riloc.app.common.KEY_HIDE_TELEPHONY
import com.riloc.app.common.KEY_HIDE_WIFI
import com.riloc.app.common.KEY_IS_PLAYING
import com.riloc.app.common.KEY_LATITUDE
import com.riloc.app.common.KEY_LONGITUDE
import com.riloc.app.common.KEY_NORMALIZE_PROVIDER
import com.riloc.app.common.KEY_RANDOMIZE_RADIUS
import com.riloc.app.common.KEY_SPEED
import com.riloc.app.common.KEY_SPEED_ACCURACY
import com.riloc.app.common.KEY_TARGET_APPS
import com.riloc.app.common.KEY_USE_ACCURACY
import com.riloc.app.common.KEY_USE_ALTITUDE
import com.riloc.app.common.KEY_USE_RANDOMIZE
import com.riloc.app.common.KEY_USE_SPEED
import com.riloc.app.common.KEY_USE_SPEED_ACCURACY
import com.riloc.app.common.KEY_USE_VERTICAL_ACCURACY
import com.riloc.app.common.KEY_VERTICAL_ACCURACY
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

import com.riloc.app.common.KEY_HIDE_PACKAGES
import com.riloc.app.common.KEY_HIDE_STACK_TRACE
import com.riloc.app.common.KEY_HOOK_NMEA
import com.riloc.app.common.KEY_HOOK_VENDOR_SDKS
import com.riloc.app.common.KEY_SIMULATE_SENSORS

/**
 * Module-side spoofed-location state.
 *
 * The manager app writes the desired coordinates and options into the remote
 * preference group; this object caches a reference to those preferences and
 * refreshes its fields on every hook invocation, then builds fully-formed fake
 * [Location] objects that carry realistic metadata and no mock-provider flags.
 */
object LocationState {

    private const val TAG = "[LocationState]"

    @Volatile
    var logger: ((priority: Int, tag: String, message: String) -> Unit)? = null

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    var isPlaying: Boolean = false
        private set

    @Volatile
    var hideMockFlag: Boolean = true
        private set

    @Volatile
    var normalizeProvider: Boolean = true
        private set

    @Volatile
    var hideAppOps: Boolean = true
        private set

    @Volatile
    var hideSettings: Boolean = true
        private set

    @Volatile
    var hideWifi: Boolean = false
        private set

    @Volatile
    var hideTelephony: Boolean = false
        private set

    @Volatile
    var enableSystemHooks: Boolean = false
        private set

    @Volatile
    var hookVendorSdks: Boolean = true
        private set

    @Volatile
    var hookNmea: Boolean = true
        private set

    @Volatile
    var hidePackages: Boolean = true
        private set

    @Volatile
    var hideStackTrace: Boolean = true
        private set

    @Volatile
    var simulateSensors: Boolean = false
        private set

    @Volatile
    private var targetApps: Set<String> = emptySet()

    var latitude: Double = DEFAULT_LATITUDE
        private set
    var longitude: Double = DEFAULT_LONGITUDE
        private set
    var gcjLatitude: Double = DEFAULT_LATITUDE
        private set
    var gcjLongitude: Double = DEFAULT_LONGITUDE
        private set
    var accuracy: Float = 0f
        private set
    var altitude: Double = 0.0
        private set
    var speed: Float = 0f
        private set
    var verticalAccuracy: Float = 0f
        private set
    var speedAccuracy: Float = 0f
        private set

    private fun log(message: String, priority: Int = Log.INFO) = logger?.invoke(priority, TAG, message)

    fun setPreferences(p: SharedPreferences?) {
        prefs = p
        update()
    }

    private const val TMP_CONF_PATH = "/data/local/tmp/riloc_loc.conf"

    fun isTarget(packageName: String?): Boolean {
        if (targetApps.isEmpty()) return true
        if (packageName == null) return false
        return packageName in targetApps || packageName == "android" || packageName == "com.autonavi.minimap" || packageName == "com.baidu.BaiduMap" || packageName == "com.tencent.mm"
    }

    /** Refreshes all cached fields from shared file or remote preferences. */
    @Synchronized
    fun update() {
        var loadedFromTmp = false
        runCatching {
            val file = java.io.File(TMP_CONF_PATH)
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                val parts = content.split(",")
                if (parts.size >= 5) {
                    val gLat = parts[0].toDoubleOrNull()
                    val gLon = parts[1].toDoubleOrNull()
                    val wLat = parts[2].toDoubleOrNull()
                    val wLon = parts[3].toDoubleOrNull()
                    val playingFlag = parts[4].toIntOrNull()
                    if (gLat != null && gLon != null && wLat != null && wLon != null &&
                        gLat.isFinite() && gLon.isFinite() && kotlin.math.abs(gLat) > 0.001 && kotlin.math.abs(gLon) > 0.001) {
                        gcjLatitude = gLat
                        gcjLongitude = gLon
                        latitude = wLat
                        longitude = wLon
                        isPlaying = (playingFlag == 1)
                        loadedFromTmp = true
                    }
                }
            }
        }

        val p = prefs ?: return
        runCatching {
            if (!loadedFromTmp) {
                isPlaying = p.getBoolean(KEY_IS_PLAYING, false)
            }
            hideMockFlag = p.getBoolean(KEY_HIDE_MOCK_FLAG, true)
            normalizeProvider = p.getBoolean(KEY_NORMALIZE_PROVIDER, true)
            hideAppOps = p.getBoolean(KEY_HIDE_APP_OPS, true)
            hideSettings = p.getBoolean(KEY_HIDE_SETTINGS, true)
            hideWifi = p.getBoolean(KEY_HIDE_WIFI, false)
            hideTelephony = p.getBoolean(KEY_HIDE_TELEPHONY, false)
            enableSystemHooks = p.getBoolean(KEY_ENABLE_SYSTEM_HOOKS, false)
            hookVendorSdks = p.getBoolean(KEY_HOOK_VENDOR_SDKS, true)
            hookNmea = p.getBoolean(KEY_HOOK_NMEA, true)
            hidePackages = p.getBoolean(KEY_HIDE_PACKAGES, true)
            hideStackTrace = p.getBoolean(KEY_HIDE_STACK_TRACE, true)
            simulateSensors = p.getBoolean(KEY_SIMULATE_SENSORS, false)
            targetApps = p.getString(KEY_TARGET_APPS, null)
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet()

            if (!loadedFromTmp) {
                val rawLat = p.getLong(KEY_LATITUDE, DEFAULT_LATITUDE.toRawBits()).let { Double.fromBits(it) }
                val rawLon = p.getLong(KEY_LONGITUDE, DEFAULT_LONGITUDE.toRawBits()).let { Double.fromBits(it) }

                if (rawLat.isFinite() && rawLon.isFinite() && kotlin.math.abs(rawLat) > 0.001 && kotlin.math.abs(rawLon) > 0.001) {
                    gcjLatitude = rawLat
                    gcjLongitude = rawLon
                    val (wgsLat, wgsLon) = CoordinateConverter.gcj02ToWgs84(rawLat, rawLon)
                    latitude = wgsLat
                    longitude = wgsLon
                } else {
                    gcjLatitude = DEFAULT_LATITUDE
                    gcjLongitude = DEFAULT_LONGITUDE
                    val (wgsLat, wgsLon) = CoordinateConverter.gcj02ToWgs84(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
                    latitude = wgsLat
                    longitude = wgsLon
                }
            }

            if (p.getBoolean(KEY_USE_ACCURACY, false)) {
                accuracy = p.getLong(KEY_ACCURACY, 0L).let { Double.fromBits(it).toFloat() }
            } else accuracy = 0f

            if (p.getBoolean(KEY_USE_ALTITUDE, false)) {
                altitude = p.getLong(KEY_ALTITUDE, 0L).let { Double.fromBits(it) }
            } else altitude = 0.0

            if (p.getBoolean(KEY_USE_SPEED, false)) {
                speed = p.getFloat(KEY_SPEED, 0f)
            } else speed = 0f

            if (p.getBoolean(KEY_USE_VERTICAL_ACCURACY, false)) {
                verticalAccuracy = p.getFloat(KEY_VERTICAL_ACCURACY, 0f)
            } else verticalAccuracy = 0f

            if (p.getBoolean(KEY_USE_SPEED_ACCURACY, false)) {
                speedAccuracy = p.getFloat(KEY_SPEED_ACCURACY, 0f)
            } else speedAccuracy = 0f
        }.onFailure { log("update failed: ${it.message}", Log.ERROR) }
    }

    /**
     * Builds a fake [Location] with the current spoofed values.
     * Metadata (time, accuracy, bearing, elapsed realtime...) is preserved from
     * [originalLocation] when available so recency/consistency checks pass.
     */
    @Synchronized
    fun createFakeLocation(originalLocation: Location?, provider: String = LocationManager.GPS_PROVIDER): Location {
        val fake = if (originalLocation == null) {
            Location(provider).apply {
                time = System.currentTimeMillis() - 300
                elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
            }
        } else {
            Location(originalLocation.provider).apply {
                time = originalLocation.time
                accuracy = originalLocation.accuracy
                bearing = originalLocation.bearing
                elapsedRealtimeNanos = originalLocation.elapsedRealtimeNanos
                runCatching { verticalAccuracyMeters = originalLocation.verticalAccuracyMeters }
                runCatching { bearingAccuracyDegrees = originalLocation.bearingAccuracyDegrees }
            }
        }

        val noiseLat = (Random.nextDouble(-1.0, 1.0) * 0.000003)
        val noiseLon = (Random.nextDouble(-1.0, 1.0) * 0.000003)
        fake.latitude = latitude + noiseLat
        fake.longitude = longitude + noiseLon

        if (accuracy > 0f) fake.accuracy = accuracy else fake.accuracy = (3.0f + Random.nextFloat() * 2.0f)
        if (altitude != 0.0) fake.altitude = altitude

        if (altitude != 0.0) fake.altitude = altitude
        if (speed > 0f) fake.speed = speed
        if (verticalAccuracy > 0f) runCatching { fake.verticalAccuracyMeters = verticalAccuracy }
        if (speedAccuracy > 0f) runCatching { fake.speedAccuracyMetersPerSecond = speedAccuracy }

        if (hideMockFlag) {
            hideMockProviderFlag(fake)
        }
        return fake
    }

    /**
     * Clears the mock-provider flag on a [Location] using every known technique:
     *  - public `setMock(false)` on API 31+
     *  - hidden `setIsFromMockProvider(false)` via HiddenApiBypass
     *  - clearing the `HAS_MOCK_PROVIDER_MASK` bit of `mFieldsMask`
     *  - stripping the `mockLocation` extra from the bundle
     */
    fun hideMockProviderFlag(location: Location) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.setMock(false)
            } else {
                org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                    location.javaClass, location, "setIsFromMockProvider", false
                )
            }
        }.onFailure { log("setIsFromMockProvider failed: ${it.message}", Log.WARN) }

        // Clear the HAS_MOCK_PROVIDER_MASK bit of mFieldsMask (AOSP Location internals).
        runCatching {
            val maskField = Location::class.java.getDeclaredField("HAS_MOCK_PROVIDER_MASK").apply { isAccessible = true }
            val fieldsMaskField = Location::class.java.getDeclaredField("mFieldsMask").apply { isAccessible = true }
            val mask = maskField.getInt(null)
            var value = fieldsMaskField.getLong(location)
            value = value and mask.inv().toLong()
            fieldsMaskField.setLong(location, value)
        }.onFailure { /* older ROMs / alternate field layout */ }

        // Strip the mockLocation extra from any attached bundle.
        runCatching {
            val extras = location.extras ?: return@runCatching
            if (extras.containsKey("mockLocation")) {
                val patched = Bundle(extras)
                patched.remove("mockLocation")
                location.extras = patched
            }
        }
    }

    /**
     * Uniformly-distributed random point inside a circle of [radiusMeters] around
     * ([lat], [lon]) using the Haversine formula (used by the "randomize" option).
     */
    fun randomizeAround(lat: Double, lon: Double, radiusMeters: Double): Pair<Double, Double> {
        val radiusRad = radiusMeters / 6371000.0
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val distance = radiusRad * sqrt(Random.nextDouble())
        val bearing = 2 * Math.PI * Random.nextDouble()
        val newLatRad = asin(sin(latRad) * cos(distance) + cos(latRad) * sin(distance) * cos(bearing))
        val newLonRad = lonRad + atan2(
            sin(bearing) * sin(distance) * cos(latRad),
            cos(distance) - sin(latRad) * sin(newLatRad)
        )
        val newLat = Math.toDegrees(newLatRad).coerceIn(-90.0, 90.0)
        var newLon = Math.toDegrees(newLonRad)
        newLon = ((newLon + 180) % 360 + 360) % 360 - 180
        return newLat to newLon
    }
}
