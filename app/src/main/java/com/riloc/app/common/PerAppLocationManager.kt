package com.riloc.app.common

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

data class PerAppLocation(
    val packageName: String,
    val lat: Double,
    val lon: Double,
    val enabled: Boolean = true,
)

/**
 * Manages per-application custom spoofed location settings (from GhostMap X & Mocci).
 */
object PerAppLocationManager {

    private const val PREF_KEY_PER_APP = "per_app_locations"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("riloc_per_app", Context.MODE_PRIVATE)
    }

    fun get(packageName: String): PerAppLocation? {
        val jsonStr = prefs.getString(PREF_KEY_PER_APP, null) ?: return null
        return runCatching {
            val root = JSONObject(jsonStr)
            if (!root.has(packageName)) return null
            val obj = root.getJSONObject(packageName)
            PerAppLocation(
                packageName = packageName,
                lat = obj.getDouble("lat"),
                lon = obj.getDouble("lon"),
                enabled = obj.optBoolean("enabled", true),
            )
        }.getOrNull()
    }

    fun getAll(): Map<String, PerAppLocation> {
        val jsonStr = prefs.getString(PREF_KEY_PER_APP, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(jsonStr)
            val map = mutableMapOf<String, PerAppLocation>()
            root.keys().forEach { pkg ->
                val obj = root.getJSONObject(pkg)
                map[pkg] = PerAppLocation(
                    packageName = pkg,
                    lat = obj.getDouble("lat"),
                    lon = obj.getDouble("lon"),
                    enabled = obj.optBoolean("enabled", true),
                )
            }
            map
        }.getOrElse { emptyMap() }
    }

    fun set(packageName: String, lat: Double, lon: Double, enabled: Boolean = true) {
        val all = getAll().toMutableMap()
        all[packageName] = PerAppLocation(packageName, lat, lon, enabled)
        saveAll(all)
    }

    fun remove(packageName: String) {
        val all = getAll().toMutableMap()
        all.remove(packageName)
        saveAll(all)
    }

    private fun saveAll(map: Map<String, PerAppLocation>) {
        val root = JSONObject()
        map.forEach { (pkg, item) ->
            val obj = JSONObject().apply {
                put("lat", item.lat)
                put("lon", item.lon)
                put("enabled", item.enabled)
            }
            root.put(pkg, obj)
        }
        prefs.edit().putString(PREF_KEY_PER_APP, root.toString()).apply()
    }
}
