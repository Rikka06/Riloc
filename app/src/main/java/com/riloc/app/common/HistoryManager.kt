package com.riloc.app.common

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val timestamp: Long,
)

/**
 * Manages search and selection location history.
 */
object HistoryManager {

    private const val PREF_KEY_HISTORY = "location_history"
    private const val MAX_HISTORY_ITEMS = 50
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("riloc_history", Context.MODE_PRIVATE)
    }

    fun getAll(): List<HistoryItem> {
        val jsonStr = prefs.getString(PREF_KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        }.getOrElse { emptyList() }
    }

    fun add(name: String, lat: Double, lon: Double) {
        val current = getAll().filterNot { kotlin.math.abs(it.lat - lat) < 0.00001 && kotlin.math.abs(it.lon - lon) < 0.00001 }.toMutableList()
        val item = HistoryItem(
            id = System.currentTimeMillis().toString(),
            name = name.ifBlank { "%.4f, %.4f".format(lat, lon) },
            lat = lat,
            lon = lon,
            timestamp = System.currentTimeMillis(),
        )
        current.add(0, item)
        if (current.size > MAX_HISTORY_ITEMS) {
            saveAll(current.take(MAX_HISTORY_ITEMS))
        } else {
            saveAll(current)
        }
    }

    fun clear() {
        prefs.edit().remove(PREF_KEY_HISTORY).apply()
    }

    private fun saveAll(list: List<HistoryItem>) {
        val array = JSONArray()
        list.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("lat", h.lat)
                put("lon", h.lon)
                put("timestamp", h.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(PREF_KEY_HISTORY, array.toString()).apply()
    }
}
